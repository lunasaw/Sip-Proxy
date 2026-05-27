package io.github.lunasaw.sip.common.sequence;

/**
 * 序列号生成器接口，用于生成SIP消息的唯一序列号。
 */
public interface GenerateSequence {

    /**
     * 生成唯一序列
     *
     * @return 序列号
     */
    Long generateSequence();
}
