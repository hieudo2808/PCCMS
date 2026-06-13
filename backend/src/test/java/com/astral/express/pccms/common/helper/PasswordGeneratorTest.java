package com.astral.express.pccms.common.helper;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordGeneratorTest {

    @Test
    void should_GeneratePassword_WithValidLength() {
        String password = PasswordGenerator.generate(12);
        assertThat(password).hasSize(12);
        assertThat(password).containsPattern(".*[a-z].*");
        assertThat(password).containsPattern(".*[A-Z].*");
        assertThat(password).containsPattern(".*[0-9].*");
        assertThat(password).containsPattern(".*[@$!%*?&].*");
    }

    @Test
    void should_ThrowException_when_GeneratePassword_WithInvalidLength() {
        assertThatThrownBy(() -> PasswordGenerator.generate(7))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_400_BAD_REQUEST);
    }
}
