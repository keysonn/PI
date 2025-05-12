package com.trafficsimulation.gui;

// Импортируем классы для отрисовки
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color; // Для цветов
import java.awt.Dimension; // Для установки предпочтительного размера

// Импортируем классы модели, которые будем рисовать
import com.trafficsimulation.model.Road;
import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.TrafficLight;
import com.trafficsimulation.model.RoadSign;

/**
 * Панель, отвечающая за визуализацию (отрисовку) состояния симуляции.
 * Наследуется от JPanel, стандартного компонента Swing для рисования.
 */
public class SimulationPanel extends JPanel {

    private Road currentRoad; // Ссылка на текущее состояние дороги
    private double currentTime;   // Текущее время симуляции для отображения

    public SimulationPanel() {
        // Устанавливаем предпочтительный размер панели, чтобы она корректно отображалась во фрейме
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.DARK_GRAY); // Задаем цвет фона
        System.out.println("SimulationPanel создана.");
    }

    /**
     * Метод, вызываемый движком симуляции для обновления данных перед перерисовкой.
     * @param road Новое состояние дороги.
     * @param time Текущее время симуляции.
     */
    public void updateSimulationState(Road road, double time) {
        this.currentRoad = road;
        this.currentTime = time;
        this.repaint(); // Говорим Swing, что панель нужно перерисовать
    }

    /**
     * Главный метод отрисовки. Вызывается системой Swing, когда панель нужно перерисовать.
     * Не вызывайте его напрямую, используйте repaint().
     * @param g Графический контекст, на котором происходит рисование.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Обязательно вызываем метод родителя для очистки фона

        // Преобразуем Graphics в Graphics2D для большего контроля
        Graphics2D g2d = (Graphics2D) g;

        // --- Начало отрисовки ---
        if (currentRoad == null) {
            // Если симуляция еще не запущена или дорога не создана
            g2d.setColor(Color.WHITE);
            g2d.drawString("Симуляция не запущена...", 50, 50);
            return; // Больше ничего не рисуем
        }

        // TODO: Добавить реальную отрисовку дороги, машин, светофоров, знаков
        // Пример простой отрисовки:
        drawRoad(g2d);
        drawCars(g2d);
        drawTrafficLights(g2d);
        drawSigns(g2d);
        drawInfo(g2d); // Отображение времени и кол-ва машин

    } // конец paintComponent

    // --- Вспомогательные методы отрисовки (пока заглушки) ---

    private void drawRoad(Graphics2D g2d) {
        // TODO: Нарисовать дорогу (прямоугольник, полосы)
        int roadWidth = 60 * currentRoad.getNumberOfLanes(); // Примерная ширина полосы 60 пикс
        int roadY = getHeight() / 2 - roadWidth / 2; // Центрируем дорогу по вертикали
        g2d.setColor(Color.GRAY);
        g2d.fillRect(0, roadY, getWidth(), roadWidth); // Рисуем дорогу на всю ширину панели

        // Рисуем разметку (просто линии)
        g2d.setColor(Color.WHITE);
        for (int i = 1; i < currentRoad.getNumberOfLanes(); i++) {
            int lineY = roadY + i * (roadWidth / currentRoad.getNumberOfLanes());
            // Пунктирная линия (если есть поддержка)
            // Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
            // g2d.setStroke(dashed);
            g2d.drawLine(0, lineY, getWidth(), lineY);
        }
        // Сплошная линия для осевой, если 2 направления
        if (currentRoad.getNumberOfDirections() == 2) {
            int centerLineY = roadY + roadWidth / 2;
            // g2d.setStroke(new BasicStroke(2)); // Сделать линию толще
            g2d.setColor(Color.YELLOW);
            g2d.drawLine(0, centerLineY, getWidth(), centerLineY);
        }
    }

    private void drawCars(Graphics2D g2d) {
        // TODO: Нарисовать машины в их позициях
        g2d.setColor(Color.BLUE);
        int roadWidth = 60 * currentRoad.getNumberOfLanes();
        int roadY = getHeight() / 2 - roadWidth / 2;
        int laneHeight = roadWidth / currentRoad.getNumberOfLanes();

        for (Car car : currentRoad.getCars()) {
            // Преобразуем позицию машины (метры) в координату X (пиксели)
            // Масштаб: вся длина дороги currentRoad.getLength() соответствует ширине панели getWidth()
            int carX = (int) ( (car.getPosition() / currentRoad.getLength()) * getWidth() );

            // Преобразуем полосу машины в координату Y
            int carY = roadY + car.getLaneIndex() * laneHeight + laneHeight / 4; // Центрируем машину на полосе
            int carWidth = 20; // Ширина машины в пикселях
            int carHeight = laneHeight / 2; // Высота машины в пикселях

            g2d.fillRect(carX - carWidth / 2, carY, carWidth, carHeight); // Рисуем машину как прямоугольник
            // Можно добавить ID машины
            // g2d.setColor(Color.WHITE);
            // g2d.drawString(String.valueOf(car.getId()), carX - carWidth / 2, carY + carHeight / 2);
            // g2d.setColor(Color.BLUE);
        }
    }

    private void drawTrafficLights(Graphics2D g2d) {
        // TODO: Нарисовать светофоры
        for (TrafficLight light : currentRoad.getTrafficLights()) {
            int lightX = (int) ( (light.getPosition() / currentRoad.getLength()) * getWidth() );
            int lightY = getHeight() / 2 - 50; // Над дорогой
            int lightSize = 15;

            // Рисуем кружок соответствующего цвета
            switch (light.getCurrentState()) {
                case RED: g2d.setColor(Color.RED); break;
                case YELLOW: g2d.setColor(Color.YELLOW); break;
                case GREEN: g2d.setColor(Color.GREEN); break;
                default: g2d.setColor(Color.BLACK); break;
            }
            g2d.fillOval(lightX - lightSize / 2, lightY, lightSize, lightSize);
        }
    }

    private void drawSigns(Graphics2D g2d) {
        // TODO: Нарисовать знаки
        g2d.setColor(Color.CYAN);
        for(RoadSign sign : currentRoad.getRoadSigns()){
            int signX = (int) ( (sign.getPosition() / currentRoad.getLength()) * getWidth() );
            int signY = getHeight() / 2 + 50; // Под дорогой
            g2d.drawString(sign.getType().name().replace("_"," "), signX, signY); // Пишем тип знака
        }
    }

    private void drawInfo(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.format("Время: %.2f c", currentTime), 10, 20);
        g2d.drawString("Машин: " + (currentRoad != null ? currentRoad.getCars().size() : 0), 10, 40);
    }

} // конец класса SimulationPanel