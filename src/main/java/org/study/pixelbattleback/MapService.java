package org.study.pixelbattleback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.study.pixelbattleback.dto.Map;
import org.study.pixelbattleback.dto.PixelRequest;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class MapService {
    private static final Logger logger = LoggerFactory.getLogger(MapService.class);

    public static final String MAP_BIN = "map.bin";

    private final static int width = 100;

    private final static int height = 100;

    private final int[] colors;

    private volatile boolean isChanged;

    private final static ReadWriteLock[] locks;

    static {
        locks = new ReentrantReadWriteLock[100 * 100];
        for (int i = 0; i < 10000; i++) {
            locks[i] = new ReentrantReadWriteLock();
        }
    }

    /**
     * Пытаемся загрузить карту из файла на старте, или же начинаем с пустой карты
     */
    public MapService() {
        Map tmp = new Map();
        tmp.setColors(new int[width * height]);
        try (FileInputStream fileInputStream = new FileInputStream(MAP_BIN);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            Object o = objectInputStream.readObject();
            tmp = (Map) o;
        } catch (Exception e) {
            logger.error("Загрузка не удалась, начинаем с пустой карты. " + e.getMessage(), e);
        }
        colors = tmp.getColors();
    }

    /**
     * Окрашивание пикселя
     *
     * @param pixel
     * @return
     */
    public boolean draw(PixelRequest pixel) {
        int x = pixel.getX();
        int y = pixel.getY();
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }
        setValueByID(y * width + x, pixel.getColor());
        isChanged = true;
        return true;
    }

    /**
     * Чтение всей карты
     *
     * @return
     */
    private int[] getColors() {
        int[] copy = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            locks[i].readLock().lock();
            copy[i] = colors[i];
            locks[i].readLock().unlock();
        }
        return copy;
    }

    public Map getMap() {
        Map mapObj = new Map();
        mapObj.setColors(getColors());
        mapObj.setWidth(width);
        mapObj.setHeight(height);
        return mapObj;
    }

    /**
     * Периодически сохраняем карту в файл
     */
    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.SECONDS)
    public void writeToFile() {
        if (!isChanged) {
            return;
        }
        System.out.println("SAVE");
        isChanged = false;
        try (FileOutputStream fileOutputStream = new FileOutputStream(MAP_BIN);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(getMap());
            logger.info("Карта сохранена в файле {}", MAP_BIN);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void setValueByID(int key, int value) {
        locks[key].writeLock().lock();
        colors[key] = value;
        locks[key].writeLock().unlock();
    }

}
