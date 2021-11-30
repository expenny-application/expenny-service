package io.ducket.api.domain.service

import domain.model.account.AccountType
import io.ducket.api.*
import io.ducket.api.domain.controller.account.AccountCreateDto
import io.ducket.api.domain.controller.follow.FollowUserDto
import io.ducket.api.domain.controller.follow.FollowerDto
import io.ducket.api.domain.controller.follow.FollowingDto
import io.ducket.api.domain.controller.user.UserDto
import io.ducket.api.domain.controller.user.UserSignInDto
import io.ducket.api.domain.controller.user.UserSignUpDto
import io.ducket.api.domain.controller.user.UserUpdateDto
import io.ducket.api.domain.repository.AccountRepository
import io.ducket.api.domain.repository.FollowRepository
import io.ducket.api.domain.repository.UserRepository
import io.ducket.api.plugins.AuthenticationException
import io.ducket.api.plugins.DuplicateEntityError
import io.ducket.api.plugins.InvalidDataError
import io.ducket.api.plugins.NoEntityFoundError
import io.ktor.http.content.*
import org.mindrot.jbcrypt.BCrypt
import java.io.File

class UserService(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val followRepository: FollowRepository,
): FileService() {
    private val logger = getLogger()

    fun getUser(userId: String): UserDto {
        return userRepository.findOne(userId)?.let { UserDto(it) } ?: throw NoEntityFoundError("No such user was found")
    }

    fun signUp(reqObj: UserSignUpDto): UserDto {
        userRepository.findOneByEmail(reqObj.email)?.let {
            throw DuplicateEntityError("Such email has already been taken")
        }

        userRepository.create(reqObj).also { newUser ->
            try {
                accountRepository.create(newUser.id,
                    AccountCreateDto(
                        name = "Wallet",
                        notes = "Account in ${newUser.mainCurrency.name}",
                        currencyId = newUser.mainCurrency.id,
                        accountType = AccountType.CASH
                    )
                )
            } catch (e: Exception) {
                logger.error("Cannot create default user account", e)
            }

            return UserDto(newUser)
        }
    }

    fun signIn(reqObj: UserSignInDto): UserDto {
        val foundUser = userRepository.findOneByEmail(reqObj.email) ?: throw AuthenticationException("The user doesn't exist")

        return UserDto(foundUser).takeIf { BCrypt.checkpw(reqObj.password, foundUser.passwordHash) }
            ?: throw AuthenticationException("The password is incorrect")
    }

    fun updateUser(userId: String, reqObj: UserUpdateDto): UserDto {
        return userRepository.updateOne(userId, reqObj)?.let { UserDto(it) }
            ?: throw Exception("Cannot update user entity")
    }

    fun deleteUser(userId: String): Boolean {
        return userRepository.deleteOne(userId)
    }

    fun deleteUserImage(userId: String, imageId: String): Boolean {
        return userRepository.findImage(userId, imageId)?.let { image ->
            userRepository.deleteImage(imageId).takeIf {
                deleteLocalFile(image.filePath)
            }
        } ?: throw NoEntityFoundError("No such image was found")
    }

    fun downloadUserImage(userId: String, imageId: String): File {
        val image = userRepository.findImage(userId, imageId)
            ?: throw NoEntityFoundError("No such image was found")

        return getLocalFile(image.filePath)
            ?: throw NoEntityFoundError("No such file was found")
    }

    fun uploadUserImage(userUuid: String, multipartData: List<PartData>) {
        val files = pullAttachments(multipartData)
            .takeIf { it.size == 1 } ?: throw InvalidDataError("Only 1 image is allowed")

        val newFile = createLocalAttachmentFile(files[0].first.extension, files[0].second)

        userRepository.createImage(userUuid, newFile)
    }

    fun createUserFollowRequest(userUuid: String, reqObj: FollowUserDto) : FollowingDto {
        val userToFollow = getUser(reqObj.userId)

        return FollowingDto(followRepository.createRequest(userUuid, userToFollow.id))
    }

    fun approveUserFollowRequest(userUuid: String, followRequestId: String) : FollowerDto {
        val followRequest = followRepository.findOneByFollowerUser(userUuid, followRequestId)
            ?: throw NoEntityFoundError("No such follow request was found")

        return followRepository.approveRequest(userUuid, followRequest.id)?.let { FollowerDto(it) }
            ?: throw Exception("Cannot approve follow request")
    }

    fun unfollowUser(userUuid: String, followId: String) : Boolean {
        val follow = followRepository.findOneByFollowerUser(userUuid, followId)
            ?: throw NoEntityFoundError("No such follow was found")

        return followRepository.unfollow(userUuid, follow.id)
    }

    fun getUserFollowing(userUuid: String) : List<FollowingDto> {
        return followRepository.findFollowingByUser(userUuid).map { FollowingDto(it) }
    }

    fun getUserFollowers(userUuid: String) : List<FollowerDto> {
        return followRepository.findFollowersByUser(userUuid).map { FollowerDto(it) }
    }
}