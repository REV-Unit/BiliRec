package moe.peanutmelonseedbigalmond.bilirec.network.api.response

data class BaseResponse<Response>(
    val code: Int,
    val message: String,
    val data: Response
)
