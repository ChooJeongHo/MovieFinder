package com.choo.moviefinder.domain.model

sealed class DomainException(message: String, cause: Throwable) : Exception(message, cause) {
    class NetworkError(cause: Throwable) : DomainException("네트워크 오류", cause)
    class Timeout(cause: Throwable) : DomainException("요청 시간 초과", cause)
    class SslError(cause: Throwable) : DomainException("SSL 오류", cause)
    class ServerError(val code: Int, cause: Throwable) : DomainException("서버 오류 ($code)", cause)
    class Unauthorized(cause: Throwable) : DomainException("인증 오류", cause)
    class NotFound(cause: Throwable) : DomainException("리소스 없음", cause)
    class RateLimited(cause: Throwable) : DomainException("요청 제한 초과", cause)
    class ParseError(cause: Throwable) : DomainException("응답 파싱 오류", cause)
    class Unknown(cause: Throwable) : DomainException("알 수 없는 오류", cause)
}
