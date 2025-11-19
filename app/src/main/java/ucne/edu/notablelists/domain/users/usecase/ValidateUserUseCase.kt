package ucne.edu.notablelists.domain.users.usecase

import ucne.edu.notablelists.data.remote.Resource
import javax.inject.Inject

class ValidateUserUseCase @Inject constructor(){

    operator fun invoke(username: String, password: String): Resource<Unit> {
        return when {
            username.isBlank() || password.isBlank() ->
                Resource.Error("Usuario y contraseña no pueden estar vacíos")

            username.length > 16 ->
                Resource.Error("El usuario no puede tener más de 16 caracteres")

            username.length < 4 ->
                Resource.Error("El usuario no puede tener menos de 4 caracteres")

            username.contains(" ") ->
                Resource.Error("El usuario no puede contener espacios")

            password.contains(" ") ->
                Resource.Error("La contraseña no puede contener espacios")

            !isValidUsername(username) ->
                Resource.Error("El usuario no puede contener símbolos")

            password.length < 8 ->
                Resource.Error("La contraseña debe tener al menos 8 caracteres")

            password.length > 64 ->
                Resource.Error("La contraseña no puede tener más de 64 caracteres")

            !isValidPassword(password) ->
                Resource.Error("La contraseña debe incluir mayúscula, minúscula y número")

            else -> Resource.Success(Unit)
        }
    }

    private fun isValidUsername(username: String): Boolean {
        val usernameRegex = "^[a-zA-Z0-9]*$".toRegex()
        return username.matches(usernameRegex)
    }

    private fun isValidPassword(password: String): Boolean {
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }

        return hasUpperCase && hasLowerCase && hasDigit
    }
}