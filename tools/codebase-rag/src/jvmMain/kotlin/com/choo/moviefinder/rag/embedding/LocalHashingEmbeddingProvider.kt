package com.choo.moviefinder.rag.embedding

import kotlin.math.sqrt

/**
 * API 키 없이 동작하는 로컬 임베딩. 실제 신경망 임베딩은 아니고, 코드 식별자를
 * camelCase/snake_case로 토큰화한 뒤 feature hashing(hashing trick)으로 고정 차원
 * 벡터에 TF 가중치를 누적하는 "해시된 TF" 벡터다 — Vowpal Wabbit 등에서 쓰는 기법과 동일한 원리.
 *
 * 진짜 의미 기반 임베딩보다 품질은 떨어지지만(동의어/문맥을 모름), 코드 검색에서
 * 결정적인 신호인 "식별자 토큰 중복"은 그대로 포착하므로 이 프로젝트 규모의 데모에는 충분하다.
 */
class LocalHashingEmbeddingProvider(
    override val dimension: Int = 512,
) : EmbeddingProvider {

    override val name: String = "local-hashing-tf-$dimension"

    override fun embedDocuments(texts: List<String>): List<FloatArray> = texts.map { embed(it) }

    override fun embedQuery(text: String): FloatArray = embed(text)

    private fun embed(text: String): FloatArray {
        val tokens = tokenize(text)
        if (tokens.isEmpty()) return FloatArray(dimension)

        val vector = FloatArray(dimension)
        for (token in tokens) {
            val bucket = fnv1aHash(token).mod(dimension)
            // 같은 토큰이 여러 번 나올수록 반환값이 줄어들도록 sqrt로 완만하게 증가(포화) — 흔한 토큰이
            // 벡터를 지배하는 것을 억제.
            vector[bucket] += 1f
        }
        for (i in vector.indices) {
            if (vector[i] > 0f) vector[i] = sqrt(vector[i].toDouble()).toFloat()
        }
        return l2Normalize(vector)
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSq = 0.0
        for (v in vector) sumSq += v.toDouble() * v
        if (sumSq == 0.0) return vector
        val norm = sqrt(sumSq).toFloat()
        return FloatArray(vector.size) { vector[it] / norm }
    }

    private fun fnv1aHash(token: String): Int {
        var hash = -0x7ee3623b // FNV offset basis (2166136261) as signed Int
        for (c in token) {
            hash = hash xor c.code
            hash *= 0x01000193 // FNV prime
        }
        return hash and 0x7fffffff // 항상 양수
    }

    companion object {
        private val CAMEL_BOUNDARY = Regex("(?<=[a-z0-9])(?=[A-Z])")

        // 한글 등 비-ASCII 문자도 토큰 문자로 인정해야 함 - 이 프로젝트 코드/질문은 한글 비중이 큼.
        // [a-zA-Z0-9]만 허용하면 한글 질문이 전부 구분자로 잘려나가 빈 토큰 목록(=0벡터)이 되는 버그가 있었음.
        private val NON_ALNUM = Regex("[^\\p{L}\\p{N}]+")
        private val STOPWORDS = setOf(
            "fun", "val", "var", "class", "object", "interface", "override", "private", "internal",
            "public", "protected", "suspend", "import", "package", "return", "if", "else", "when",
            "for", "while", "true", "false", "null", "this", "super", "companion", "the", "a", "an",
            "is", "in", "to", "of", "and", "or", "not", "kotlin", "java", "androidx", "com", "choo",
            "moviefinder", "also", "let", "apply", "run", "with",
        )

        // 교착어 특성상 명사에 조사가 그대로 붙어("추가는", "파일들을") 코드 주석의 원형("추가", "파일")과
        // 해시가 어긋나는 문제를 완화하기 위한 최소한의 조사 스트리핑. 원본 토큰은 그대로 두고
        // 조사를 뗀 변형을 "추가"만 하므로(교체 아님) 잘못 스트리핑돼도 원래 신호는 남는다.
        private val PARTICLES_2 = setOf("들을", "들이", "들은", "들의", "들도", "에서", "에게", "한테", "까지", "부터")
        private val PARTICLES_1 = setOf("은", "는", "이", "가", "을", "를", "의", "에", "로", "와", "과", "도", "만")
    }

    internal fun tokenize(text: String): List<String> {
        return NON_ALNUM.split(text)
            .flatMap { it.split(CAMEL_BOUNDARY) }
            .map { it.lowercase() }
            // 한글 2음절 단어(추가/파일/인증 등)도 의미 있는 토큰이라 길이 기준을 1보다 크게만 둠.
            .filter { it.length > 1 && it !in STOPWORDS && it.any { c -> c.isLetter() } }
            .flatMap(::withParticleVariants)
    }

    private fun withParticleVariants(token: String): List<String> {
        if (token.none { it in '가'..'힣' }) return listOf(token)
        val variants = mutableListOf(token)
        if (token.length >= 5 && token.takeLast(2) in PARTICLES_2) variants += token.dropLast(2)
        if (token.length >= 3 && token.takeLast(1) in PARTICLES_1) variants += token.dropLast(1)
        return variants
    }
}
