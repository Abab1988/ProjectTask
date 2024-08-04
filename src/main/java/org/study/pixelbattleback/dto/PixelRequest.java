package org.study.pixelbattleback.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Контейнер запроса для окрашивания пикселя
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PixelRequest {
    private int x;

    private int y;

    /**
     * Цвета хранятся внутри числа
     *  1-8 биты - blue
     *  9-16 биты - green
     *  17-24 биты - red
     *  25-32 - не используются
     */
    private int color;
}
