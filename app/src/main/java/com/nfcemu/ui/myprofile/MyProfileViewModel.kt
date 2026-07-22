package com.nfcemu.ui.myprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcemu.data.Profile
import com.nfcemu.data.ProfileRepository
import com.nfcemu.ndefengine.NdefPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyProfileUiState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val organization: String = "",
) {
    /** Matches the mockup's `myProfileInvalid` guard: a name plus at least one contact method. */
    val isValid: Boolean get() = name.isNotBlank() && (phone.isNotBlank() || email.isNotBlank())
}

/**
 * Edits the single reserved "My Profile" row ([Profile.MY_PROFILE_ID]). Sharing it via NFC
 * reuses the exact same Transmit/HCE pipeline every other profile uses - see
 * [ProfileRepository.saveMyProfile]'s kdoc for why that row is a real [Profile].
 */
@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyProfileUiState())
    val uiState: StateFlow<MyProfileUiState> = _uiState.asStateFlow()

    init {
        val vcard = profileRepository.profiles.value
            .find { it.id == Profile.MY_PROFILE_ID }
            ?.fields as? NdefPayload.VCard
        if (vcard != null) {
            _uiState.value = MyProfileUiState(
                name = vcard.name.orEmpty(),
                phone = vcard.phones.firstOrNull().orEmpty(),
                email = vcard.emails.firstOrNull().orEmpty(),
                organization = vcard.organization.orEmpty(),
            )
        }
    }

    fun updateName(value: String) = _uiState.update { it.copy(name = value) }
    fun updatePhone(value: String) = _uiState.update { it.copy(phone = value) }
    fun updateEmail(value: String) = _uiState.update { it.copy(email = value) }
    fun updateOrganization(value: String) = _uiState.update { it.copy(organization = value) }

    fun save() {
        viewModelScope.launch { profileRepository.saveMyProfile(currentVCard()) }
    }

    fun shareViaNfc() {
        viewModelScope.launch {
            profileRepository.saveMyProfile(currentVCard())
            profileRepository.setActive(Profile.MY_PROFILE_ID)
        }
    }

    private fun currentVCard(): NdefPayload.VCard {
        val state = _uiState.value
        return NdefPayload.VCard(
            name = state.name.takeIf { it.isNotBlank() },
            phones = listOfNotNull(state.phone.takeIf { it.isNotBlank() }),
            emails = listOfNotNull(state.email.takeIf { it.isNotBlank() }),
            organization = state.organization.takeIf { it.isNotBlank() },
        )
    }
}
