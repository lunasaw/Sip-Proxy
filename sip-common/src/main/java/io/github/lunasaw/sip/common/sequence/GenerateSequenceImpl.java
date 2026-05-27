package io.github.lunasaw.sip.common.sequence;

import lombok.SneakyThrows;

import java.util.HashSet;
import java.util.Set;

/**
 * 基于时间戳的序列号生成器实现。
 */
public class GenerateSequenceImpl implements GenerateSequence {

    /**
     * 获取基于当前时间戳的序列号。
     *
     * @return 序列号
     */
    public static long getSequence() {
        long timestamp = System.currentTimeMillis();
        return (timestamp & 0x3FFF) % Integer.MAX_VALUE;
    }

    @Override
    public Long generateSequence() {
        return getSequence();
    }


    @SneakyThrows
    public static void main(String[] args) {

        Set<Long> list = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            Thread.sleep(1);
            long sequence = getSequence();
            System.out.println(sequence);
            list.add(sequence);
        }

        System.out.println(list.size());
    }
}
