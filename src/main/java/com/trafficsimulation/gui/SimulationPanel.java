package com.trafficsimulation.gui;

import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road;
import com.trafficsimulation.model.RoadType;
import com.trafficsimulation.model.TrafficLight;
import com.trafficsimulation.model.TrafficLightState;

import javax.swing.*;
import java.awt.*;
// import java.util.List; // Не используется напрямую здесь

public class SimulationPanel extends JPanel {

    private Road road;
    private double simulationTime;

    private boolean placementModeActive = false;
    private String placementHint = null;

    private static final int ROAD_RENDER_HEIGHT = 80;
    private static final Color ROAD_COLOR = Color.DARK_GRAY;
    private static final Color LANE_SEPARATOR_COLOR = Color.YELLOW;
    private static final Color GRASS_COLOR = new Color(34, 139, 34);

    private static final int TRAFFIC_LIGHT_VISUAL_WIDTH = 20; // Уменьшил для более аккуратного вида без коробки
    private static final int TRAFFIC_LIGHT_POLE_HEIGHT = 30;
    private static final Color TRAFFIC_LIGHT_POLE_COLOR = Color.DARK_GRAY;

    private static final int CAR_RENDER_WIDTH = 30;  // Ширина машинки
    private static final int CAR_RENDER_HEIGHT = 16; // Высота машинки
    private static final int CAR_ARC_RADIUS = 8;     // Скругление углов машинки


    public SimulationPanel() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(GRASS_COLOR);
    }

    public void updateSimulationState(Road road, double simulationTime) {
        this.road = road;
        this.simulationTime = simulationTime;
        repaint();
    }

    public void setPlacementMode(boolean active, String hint) {
        this.placementModeActive = active;
        this.placementHint = hint;
        if (active) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (road == null) {
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            String message = "Дорога не инициализирована. Задайте параметры и запустите генерацию.";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(message, getWidth() / 2 - fm.stringWidth(message) / 2, getHeight() / 2);
            return;
        }

        int roadY = getHeight() / 2 - ROAD_RENDER_HEIGHT / 2;
        drawRoadSurface(g2d, roadY);

        if (road.getCars() != null && !road.getCars().isEmpty()) {
            for (Car car : road.getCars()) {
                drawCar(g2d, car, roadY);
            }
        }

        if (road.getTrafficLights() != null && !road.getTrafficLights().isEmpty()) {
            int panelWidth = getWidth();
            for (TrafficLight light : road.getTrafficLights()) {
                int lightScreenX;
                int lightScreenY = roadY - (TRAFFIC_LIGHT_VISUAL_WIDTH) - 5; // Y для светофора над дорогой (с учетом диаметра сигнала)
                if (light.getTargetDirection() == 1 && road.getType() != RoadType.TUNNEL && road.getNumberOfDirections() == 2) {
                    lightScreenY = roadY + ROAD_RENDER_HEIGHT + 5 + TRAFFIC_LIGHT_POLE_HEIGHT; // Y для светофора под дорогой
                }


                double roadModelLength = road.getLength();
                if (roadModelLength > 0) {
                    lightScreenX = (int) ((light.getPosition() / roadModelLength) * panelWidth);
                    lightScreenX -= TRAFFIC_LIGHT_VISUAL_WIDTH / 2;
                    lightScreenX = Math.max(0, Math.min(lightScreenX, panelWidth - TRAFFIC_LIGHT_VISUAL_WIDTH));
                } else {
                    lightScreenX = 10;
                }
                drawTrafficLight(g2d, light, lightScreenX, lightScreenY);
            }
        }
        drawInfoPanel(g2d);

        if (placementModeActive && placementHint != null) {
            g2d.setColor(new Color(0,0,200, 200));
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            String hintText = "РЕЖИМ: " + placementHint + ". Кликните на дорогу для размещения.";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(hintText);
            g2d.fillRect(getWidth() / 2 - textWidth / 2 - 10, getHeight() - 65 - fm.getAscent(), textWidth + 20, fm.getHeight() + 5);
            g2d.setColor(Color.WHITE);
            g2d.drawString(hintText, getWidth() / 2 - textWidth / 2, getHeight() - 60);
        }
    }

    private void drawRoadSurface(Graphics2D g2d, int roadY) {
        g2d.setColor(ROAD_COLOR);
        g2d.fillRect(0, roadY, getWidth(), ROAD_RENDER_HEIGHT);
        g2d.setColor(LANE_SEPARATOR_COLOR);

        int lanesPerDirection;
        if (road.getNumberOfDirections() > 0 && road.getNumberOfLanes() > 0) {
            lanesPerDirection = road.getNumberOfLanes() / road.getNumberOfDirections();
        } else {
            lanesPerDirection = road.getNumberOfLanes();
        }
        if (lanesPerDirection == 0) lanesPerDirection = 1;

        int numLanesTotalOnScreen = road.getNumberOfLanes();
        if (numLanesTotalOnScreen == 0) numLanesTotalOnScreen = 1;
        int laneVisualHeight = ROAD_RENDER_HEIGHT / numLanesTotalOnScreen;

        if (road.getType() == RoadType.TUNNEL) {
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(0, roadY + ROAD_RENDER_HEIGHT / 2, getWidth(), roadY + ROAD_RENDER_HEIGHT / 2);
        } else {
            if (road.getNumberOfDirections() == 2) {
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(0, roadY + (lanesPerDirection * laneVisualHeight),
                        getWidth(), roadY + (lanesPerDirection * laneVisualHeight));
            }
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    0, new float[]{15, 10}, 0));
            for (int dir = 0; dir < road.getNumberOfDirections(); dir++) {
                for (int lane = 1; lane < lanesPerDirection; lane++) {
                    int lineY = roadY + (dir * lanesPerDirection * laneVisualHeight) + (lane * laneVisualHeight);
                    g2d.drawLine(0, lineY, getWidth(), lineY);
                }
            }
        }
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(0, roadY, getWidth(), roadY);
        g2d.drawLine(0, roadY + ROAD_RENDER_HEIGHT, getWidth(), roadY + ROAD_RENDER_HEIGHT);
    }

    private void drawTrafficLight(Graphics2D g2d, TrafficLight light, int screenX, int screenY_base_of_signal) {
        // screenY_base_of_signal - это Y координата, где должен начаться сам СИГНАЛ (круг)
        int signalDiameter = TRAFFIC_LIGHT_VISUAL_WIDTH; // Диаметр сигнала равен ширине "светофора"
        int signalRadius = signalDiameter / 2;

        // Координаты верхнего левого угла овала сигнала
        int signalOvalX = screenX;
        int signalOvalY = screenY_base_of_signal;


        // Столб
        g2d.setColor(TRAFFIC_LIGHT_POLE_COLOR);
        int poleX = screenX + signalRadius - 2; // Центрируем столб под сигналом
        if (screenY_base_of_signal < getHeight() / 2) { // Светофор над дорогой
            g2d.fillRect(poleX, signalOvalY + signalDiameter, 4, TRAFFIC_LIGHT_POLE_HEIGHT);
        } else { // Светофор под дорогой
            g2d.fillRect(poleX, signalOvalY - TRAFFIC_LIGHT_POLE_HEIGHT, 4, TRAFFIC_LIGHT_POLE_HEIGHT);
        }

        // Сам сигнал (без корпуса)
        Color signalColor;
        if (light.getCurrentState() == TrafficLightState.GREEN) {
            signalColor = Color.GREEN.brighter();
        } else if (light.getCurrentState() == TrafficLightState.RED) {
            signalColor = Color.RED.brighter();
        } else {
            signalColor = Color.GRAY;
        }
        g2d.setColor(signalColor);
        g2d.fillOval(signalOvalX, signalOvalY, signalDiameter, signalDiameter);

        // Оставшееся время
        Color textColor = Color.WHITE;
        if (light.getCurrentState() == TrafficLightState.GREEN) textColor = Color.BLACK;

        g2d.setColor(textColor);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(9, signalRadius - 2)));
        String timeText = String.format("%.0f", light.getRemainingTime());
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(timeText);
        g2d.drawString(timeText,
                signalOvalX + signalRadius - textWidth / 2,
                signalOvalY + signalRadius + fm.getAscent() / 2 -1);
    }


    private void drawCar(Graphics2D g2d, Car car, int roadSurfaceY) {
        double roadModelLength = road.getLength();
        int panelWidth = getWidth();
        int carScreenX;

        if (roadModelLength > 0) {
            carScreenX = (int) ((car.getPosition() / roadModelLength) * panelWidth);
        } else {
            carScreenX = (car.getDirection() == 0) ? -CAR_RENDER_WIDTH : panelWidth;
        }
        carScreenX -= CAR_RENDER_WIDTH / 2;

        int lanesPerDirVisual;
        if (road.getNumberOfDirections() > 0 && road.getNumberOfLanes() > 0) {
            lanesPerDirVisual = road.getNumberOfLanes() / road.getNumberOfDirections();
        } else {
            lanesPerDirVisual = road.getNumberOfLanes();
        }
        if (lanesPerDirVisual == 0) lanesPerDirVisual = 1;

        int totalLanesOnScreenVisual = road.getNumberOfLanes();
        if(totalLanesOnScreenVisual == 0) totalLanesOnScreenVisual = 1;
        int laneVisualHeight = ROAD_RENDER_HEIGHT / totalLanesOnScreenVisual;
        int carScreenY = roadSurfaceY + (car.getLaneIndex() * laneVisualHeight) + (laneVisualHeight / 2 - CAR_RENDER_HEIGHT / 2);

        Color carColor;
        // Возвращаем цвета как было (или подберите свои)
        // Например, синий для направления 0 и фиолетовый для направления 1
        if (car.getDirection() == 0) {
            carColor = new Color(60, 100, 200); // Более приятный синий
        } else {
            carColor = new Color(150, 60, 180); // Фиолетовый
        }
        g2d.setColor(carColor);
        // Рисуем скругленный прямоугольник
        g2d.fillRoundRect(carScreenX, carScreenY, CAR_RENDER_WIDTH, CAR_RENDER_HEIGHT, CAR_ARC_RADIUS, CAR_ARC_RADIUS);

        // Окантовка для лучшей видимости
        g2d.setColor(carColor.darker());
        g2d.drawRoundRect(carScreenX, carScreenY, CAR_RENDER_WIDTH, CAR_RENDER_HEIGHT, CAR_ARC_RADIUS, CAR_ARC_RADIUS);


        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        String speedText = String.format("%.0f", car.getCurrentSpeed() * 3.6);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(speedText,
                carScreenX + CAR_RENDER_WIDTH / 2 - fm.stringWidth(speedText) / 2,
                carScreenY + CAR_RENDER_HEIGHT / 2 + fm.getAscent() / 2 - 2);
    }

    private void drawInfoPanel(Graphics2D g2d) {
        g2d.setColor(new Color(0,0,0,150));
        g2d.fillRect(5,5, 250, 50);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));

        String simTimeStr = String.format("Время: %.1f c", simulationTime);
        String carCountStr = "Машин: " + (road.getCars() != null ? road.getCars().size() : 0);
        String roadInfoStr;
        if (road.getType() == RoadType.TUNNEL) {
            roadInfoStr = "Дорога: " + road.getType() + " (реверс.)";
        } else {
            int lanesPerDirInfo;
            if (road.getNumberOfDirections() > 0 && road.getNumberOfLanes() > 0) {
                lanesPerDirInfo = road.getNumberOfLanes() / road.getNumberOfDirections();
            } else {
                lanesPerDirInfo = road.getNumberOfLanes();
            }
            if (lanesPerDirInfo == 0) lanesPerDirInfo = 1;
            roadInfoStr = "Дорога: " + road.getType() + ", " +
                    (road.getNumberOfDirections() == 1 ? "1-стор." : "2-стор.") + ", " +
                    lanesPerDirInfo + " п./напр.";
        }
        g2d.drawString(simTimeStr, 10, 20);
        g2d.drawString(carCountStr, 10, 35);
        g2d.drawString(roadInfoStr, 10, 50);
    }
}