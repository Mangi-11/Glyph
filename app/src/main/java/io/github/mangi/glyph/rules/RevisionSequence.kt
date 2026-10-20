package io.github.mangi.glyph.rules

internal object RevisionSequence {
    fun next(
        storedRevision: Long,
        lastIssuedRevision: Long,
    ): Long {
        val base = maxOf(storedRevision, lastIssuedRevision)
        check(base < Long.MAX_VALUE) { "Configuration revision is exhausted" }
        return base + 1L
    }
}
