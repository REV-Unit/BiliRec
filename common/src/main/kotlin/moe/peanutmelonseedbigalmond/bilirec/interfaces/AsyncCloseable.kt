package moe.peanutmelonseedbigalmond.bilirec.interfaces

interface AsyncCloseable {
    suspend fun closeAsync()
}