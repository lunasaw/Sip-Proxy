package io.github.lunasaw.sip.common.sequence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateSequenceImplTest {

    @Test
    void getSequence_inValidRange() {
        long seq = GenerateSequenceImpl.getSequence();
        assertThat(seq).isGreaterThanOrEqualTo(0).isLessThan(Integer.MAX_VALUE);
    }

    @Test
    void generateSequence_delegatesToStaticMethod() {
        GenerateSequenceImpl impl = new GenerateSequenceImpl();
        Long seq = impl.generateSequence();
        assertThat(seq).isNotNull().isGreaterThanOrEqualTo(0L).isLessThan((long) Integer.MAX_VALUE);
    }

    @Test
    void getSequence_repeatedCallsDoNotThrow() {
        for (int i = 0; i < 100; i++) {
            assertThat(GenerateSequenceImpl.getSequence()).isGreaterThanOrEqualTo(0);
        }
    }
}
