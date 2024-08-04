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
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class MapService {
    private static final Logger logger = LoggerFactory.getLogger(MapService.class);

    public static final String MAP_BIN = "map.bin";

    private final int width;

    private final int height;

    private final int[] colors;

    private boolean isChanged;

    private ReentrantLock[] locks;

    private Set<ReentrantLock> locksSet = ConcurrentHashMap.newKeySet();

    /**
     * Пытаемся загрузить карту из файла на старте, или же начинаем с пустой карты
     */
    public MapService() {
        Map tmp = new Map();
        tmp.setWidth(100);
        tmp.setHeight(100);
        tmp.setColors(new int[tmp.getWidth() * tmp.getHeight()]);
        ReentrantLock[] locks = new ReentrantLock[tmp.getWidth() * tmp.getHeight()];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantLock();
        }
        try (FileInputStream fileInputStream = new FileInputStream(MAP_BIN);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            Object o = objectInputStream.readObject();
            tmp = (Map) o;
        } catch (Exception e) {
            logger.error("Загрузка не удалась, начинаем с пустой карты. " + e.getMessage(), e);
        }
        width = tmp.getWidth();
        height = tmp.getHeight();
        colors = tmp.getColors();
        this.locks = locks;
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
            copy[i] = getValueByID(i);
            releaseLock(i);
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
    public synchronized void writeToFile() {
        if (!isChanged) {
            return;
        }
        isChanged = false;
        try (FileOutputStream fileOutputStream = new FileOutputStream(MAP_BIN);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(getMap());
            logger.info("Карта сохранена в файле {}", MAP_BIN);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void attainLock(int key) {
        final ReentrantLock lock = locks[key];
        lock.lock();
        locksSet.add(lock);
    }

    private void releaseLock(ReentrantLock lock) {
        if (!locksSet.contains(lock)) {
            throw new IllegalStateException("");
        }
        locksSet.remove(lock);
        lock.unlock();
    }

    private void releaseLock(int key) {
        final ReentrantLock lock = locks[key];
        releaseLock(lock);
    }

    private void releaseLocks() {
        for (ReentrantLock reentrantLock : locksSet) {
            releaseLock(reentrantLock);
        }
    }

    public void setValueByID(int key, int value) {
        attainLock(key);
        colors[key] = value;
        releaseLock(key);
    }

    public int getValueByID(int key) {
        attainLock(key);
        return colors[key];
    }

    public void commit() {
        releaseLocks();
    }


}
