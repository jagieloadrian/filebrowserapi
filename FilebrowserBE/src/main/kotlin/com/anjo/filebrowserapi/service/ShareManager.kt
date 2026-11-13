package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.model.CreatedShare
import com.anjo.filebrowserapi.model.Share
import com.anjo.filebrowserapi.repository.ShareRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.HexFormat
import java.util.UUID

const val DEFAULT_SHARE_DAYS = 7L
const val MAX_SHARE_DAYS = 30L
private const val TOKEN_BYTES = 32
private const val NO_SHARE = "Share link does not exist or has expired"

@Service
class ShareManager(
    private val modifierUtils: PathModifierUtils,
    private val shareRepository: ShareRepository,
) {
    private val random = SecureRandom()

    fun create(
        path: String,
        days: Long?,
    ): CreatedShare {
        val validDays = days ?: DEFAULT_SHARE_DAYS
        if (validDays !in 1..MAX_SHARE_DAYS) throw BadInputException("days must be between 1 and $MAX_SHARE_DAYS")
        val file = modifierUtils.resolveEntry(path)
        if (!file.exists()) throw NotFoundException("Cannot share non existing file")
        if (!file.isFile) throw BadInputException("Only files can be shared")
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(TOKEN_BYTES).also(random::nextBytes))
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val share = Share(UUID.randomUUID(), modifierUtils.relativePath(file), now, now.plus(validDays, ChronoUnit.DAYS))
        shareRepository.insert(modifierUtils.currentUser(), share, hash(token))
        return CreatedShare(share, token)
    }

    fun list(): List<Share> = shareRepository.list(modifierUtils.currentUser())

    fun revoke(id: String) {
        val uuid = runCatching { UUID.fromString(id) }.getOrElse { throw BadInputException("Invalid share id '$id'") }
        if (!shareRepository.delete(modifierUtils.currentUser(), uuid)) throw NotFoundException("Share $id does not exist")
    }

    fun open(token: String): File {
        val (owner, path) = shareRepository.findActive(hash(token), Instant.now()) ?: throw NotFoundException(NO_SHARE)
        val file = path.takeIf { it.startsWith("/") }?.let { modifierUtils.resolveOwned(owner, it) }
        return file?.takeIf { it.isFile } ?: throw NotFoundException(NO_SHARE)
    }

    @Scheduled(cron = "0 0 3 * * *")
    fun purgeExpired() {
        shareRepository.deleteExpired(Instant.now())
    }

    private fun hash(token: String): String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))
}
