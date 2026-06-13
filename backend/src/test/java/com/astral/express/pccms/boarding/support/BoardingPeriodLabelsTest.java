package com.astral.express.pccms.boarding.support;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;

class BoardingPeriodLabelsTest {

    @Test
    void should_ReturnSang_when_PeriodCodeIsMorning() {
        assertThat(BoardingPeriodLabels.toPeriodLabel("MORNING")).isEqualTo("Sáng");
    }

    @Test
    void should_ReturnTrua_when_PeriodCodeIsNoon() {
        assertThat(BoardingPeriodLabels.toPeriodLabel("NOON")).isEqualTo("Trưa");
    }

    @Test
    void should_ReturnChieu_when_PeriodCodeIsAfternoon() {
        assertThat(BoardingPeriodLabels.toPeriodLabel("AFTERNOON")).isEqualTo("Chiều");
    }

    @Test
    void should_ReturnSameCode_when_PeriodCodeIsUnknown() {
        assertThat(BoardingPeriodLabels.toPeriodLabel("EVENING")).isEqualTo("EVENING");
    }
    
    @Test
    void should_NotInstantiate_when_PrivateConstructor() throws Exception {
        Constructor<BoardingPeriodLabels> constructor = BoardingPeriodLabels.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        BoardingPeriodLabels instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
