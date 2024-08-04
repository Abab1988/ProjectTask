package org.study.pixelbattleback.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Контейнер для хранения информации о карте
 */
@Getter
@Setter
public class Map implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int width;

    private int height;

    /**
     * Цвета хранятся внутри числа
     *  1-8 биты - blue
     *  9-16 биты - green
     *  17-24 биты - red
     *  25-32 - не используются
     */
    private int[] colors;
}
