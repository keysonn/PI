package com.trafficsimulation.gui;

import javax.swing.*;
import java.awt.*;
// ... (остальные импорты как в предыдущей версии) ...
import com.trafficsimulation.model.Road;
import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.TrafficLight;
import com.trafficsimulation.model.RoadSign;


public class SimulationPanel extends JPanel {

    private Road currentRoad;
    private double currentTime;
    private final int LANE_HEIGHT_PX = 30; // Сделаем высоту полосы константой
    private final int CAR_WIDTH_PX = 10;   // Ширина машины
    private final int CAR_HEIGHT_PX = LANE_HEIGHT_PX / 2 - 4; // Высота машины

    public SimulationPanel() {
        setPreferredSize(new Dimension(1000, 300)); // Ширина 1000, высота зависит от кол-ва полос, но зададим начальную
        setBackground(new Color(0, 50, 0)); // Темно-зеленый фон (трава)
        System.out.println("SimulationPanel создана.");
    }

    public void updateSimulationState(Road road, double time) {
        this.currentRoad = road;
        this.currentTime = time;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Сглаживание

        if (currentRoad == null) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            String text = "Симуляция не запущена...";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(text, x, y);
            return;
        }

        drawRoadSurface(g2d);
        drawLanesAndMarkings(g2d);
        drawTrafficLights(g2d);
        drawRoadSigns(g2d);
        drawCars(g2d);
        drawInfo(g2d);
    }

    private void drawRoadSurface(Graphics2D g2d) {
        int totalLanes = currentRoad.getNumberOfLanes();
        int roadPixelHeight = totalLanes * LANE_HEIGHT_PX;
        int roadY = getHeight() / 2 - roadPixelHeight / 2;

        g2d.setColor(Color.DARK_GRAY); // Цвет асфальта
        g2d.fillRect(0, roadY, getWidth(), roadPixelHeight);
    }

    private void drawLanesAndMarkings(Graphics2D g2d) {
        int totalLanes = currentRoad.getNumberOfLanes();
        int lanesPerDir = currentRoad.getNumberOfLanes() / currentRoad.getNumberOfDirections(); // Предполагаем симметрично
        int roadPixelHeight = totalLanes * LANE_HEIGHT_PX;
        int roadY = getHeight() / 2 - roadPixelHeight / 2;

        g2d.setColor(Color.WHITE);
        // Разделительные линии между полосами одного направления (пунктир)
        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        Stroke solid = new BasicStroke(1);

        for (int i = 0; i < totalLanes; i++) {
            if (i == 0) continue; // Не рисуем линию над самой верхней полосой

            int lineY = roadY + i * LANE_HEIGHT_PX;

            // Осевая линия, если 2 направления и это середина
            if (currentRoad.getNumberOfDirections() == 2 && i == lanesPerDir) {
                g2d.setStroke(new BasicStroke(2)); // Двойная сплошная или жирная желтая
                g2d.setColor(Color.YELLOW);
                g2d.drawLine(0, lineY, getWidth(), lineY);
                // Если нужна двойная сплошная:
                // g2d.drawLine(0, lineY - 1, getWidth(), lineY - 1);
                // g2d.drawLine(0, lineY + 1, getWidth(), lineY + 1);
            } else {
                g2d.setStroke(dashed);
                g2d.setColor(Color.WHITE);
                g2d.drawLine(0, lineY, getWidth(), lineY);
            }
        }
        g2d.setStroke(solid); // Возвращаем обычную линию
    }


    private void drawCars(Graphics2D g2d) {
        if (currentRoad.getCars() == null) return;

        int totalLanes = currentRoad.getNumberOfLanes();
        int roadPixelHeight = totalLanes * LANE_HEIGHT_PX;
        int roadY = getHeight() / 2 - roadPixelHeight / 2;

        for (Car car : currentRoad.getCars()) {
            // Масштабирование позиции машины на экране
            int carScreenX = (int) ((car.getPosition() / currentRoad.getLength()) * getWidth());

            // Y координата зависит от индекса полосы
            int carScreenY = roadY + car.getLaneIndex() * LANE_HEIGHT_PX + (LANE_HEIGHT_PX - CAR_HEIGHT_PX) / 2;

            if (car.getDirection() == 0) {
                g2d.setColor(Color.CYAN); // Цвет для машин слева направо
            } else {
                g2d.setColor(Color.PINK); // Цвет для машин справа налево
            }
            g2d.fillRect(carScreenX - CAR_WIDTH_PX / 2, carScreenY, CAR_WIDTH_PX, CAR_HEIGHT_PX);

            // Отрисовка ID машины (для отладки)
            // g2d.setColor(Color.BLACK);
            // g2d.drawString(String.valueOf(car.getId()), carScreenX - CAR_WIDTH_PX/2, carScreenY + CAR_HEIGHT_PX/2);
        }
    }

    private void drawTrafficLights(Graphics2D g2d) {
        if (currentRoad.getTrafficLights() == null) return;
        int roadPixelHeight = currentRoad.getNumberOfLanes() * LANE_HEIGHT_PX;
        int roadY = getHeight() / 2 - roadPixelHeight / 2;

        for (TrafficLight light : currentRoad.getTrafficLights()) {
            int lightScreenX = (int) ((light.getPosition() / currentRoad.getLength()) * getWidth());
            int lightSize = LANE_HEIGHT_PX / 2;

            // Рисуем светофор над дорогой (для направления 0) и под дорогой (для направления 1)
            // Это упрощение, в реальности светофоры могут быть сложнее
            int lightScreenYDir0 = roadY - lightSize - 5;
            // int lightScreenYDir1 = roadY + roadPixelHeight + 5;

            switch (light.getCurrentState()) {
                case RED: g2d.setColor(Color.RED); break;
                case YELLOW: g2d.setColor(Color.YELLOW); break;
                case GREEN: g2d.setColor(Color.GREEN); break;
                default: g2d.setColor(Color.BLACK); break;
            }
            g2d.fillOval(lightScreenX - lightSize / 2, lightScreenYDir0, lightSize, lightSize);
            // Если нужно дублировать для другого направления:
            // g2d.fillOval(lightScreenX - lightSize / 2, lightScreenYDir1, lightSize, lightSize);
        }
    }

    private void drawRoadSigns(Graphics2D g2d) {
        // ... (код этого метода пока можно оставить без изменений, если знаки общие)
        // ... или адаптировать их позицию Y аналогично светофорам, если они специфичны для направления
        if (currentRoad.getRoadSigns() == null) return;
        int roadPixelHeight = currentRoad.getNumberOfLanes() * LANE_HEIGHT_PX;
        int roadY = getHeight() / 2 - roadPixelHeight / 2;
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for(RoadSign sign : currentRoad.getRoadSigns()){
            int signX = (int) ( (sign.getPosition() / currentRoad.getLength()) * getWidth() );
            int signY = roadY + roadPixelHeight + 15; // Под дорогой
            g2d.drawString(sign.getType().name().replace("_"," "), signX - 15, signY);
        }
    }

    private void drawInfo(Graphics2D g2d) {
        // ... (код этого метода не меняется) ...
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString(String.format("Время: %.2f c", currentTime), 10, 20);
        g2d.drawString("Машин: " + (currentRoad != null && currentRoad.getCars() != null ? currentRoad.getCars().size() : 0), 10, 40);
        if (currentRoad != null) {
            g2d.drawString("Тип: " + currentRoad.getType() + ", Полос: " + currentRoad.getNumberOfLanes() +
                    ", Направлений: " + currentRoad.getNumberOfDirections(), 10, 60);
        }
    }
}