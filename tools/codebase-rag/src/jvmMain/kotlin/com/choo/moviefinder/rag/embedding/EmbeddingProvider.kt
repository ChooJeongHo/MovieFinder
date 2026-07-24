package com.choo.moviefinder.rag.embedding

/**
 * 임베딩 제공자 추상화. 기본값은 [LocalHashingEmbeddingProvider](API 키 불필요)이고,
 * local.properties에 VOYAGE_API_KEY가 있으면 [VoyageEmbeddingProvider](voyage-code-3)로 자동 승격된다.
 * document/query를 분리한 이유: Voyage 등 실제 임베딩 모델은 검색 품질을 위해 문서용/질의용
 * 프롬프트를 다르게 붙이는 비대칭(input_type) 임베딩을 지원하기 때문.
 */
interface EmbeddingProvider {
    val dimension: Int
    val name: String

    /** 인덱싱 시점: 코드 청크(문서) 임베딩. 배치 API를 쓸 수 있는 구현체는 오버라이드해서 효율화. */
    fun embedDocuments(texts: List<String>): List<FloatArray>

    /** 질의 시점: 사용자 질문 임베딩. */
    fun embedQuery(text: String): FloatArray
}
