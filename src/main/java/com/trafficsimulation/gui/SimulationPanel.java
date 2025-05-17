package com.trafficsimulation.gui;

import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road;
import com.trafficsimulation.model.RoadSign;
import com.trafficsimulation.model.RoadSignType;
import com.trafficsimulation.model.RoadType;
import com.trafficsimulation.model.TrafficLight;
import com.trafficsimulation.model.TrafficLightState;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class SimulationPanel extends JPanel {

    private Road road;
    private double simulationTime;

    private boolean placementModeActive = false;
    private String placementHint = null;

    private static final int ROAD_RENDER_HEIGHT = 80;
    private static final Color ROAD_COLOR = new Color(100, 100, 100);
    private static final Color LANE_SEPARATOR_COLOR = Color.WHITE;
    private static final Color CENTER_LINE_COLOR = Color.YELLOW;
    private static final Color EDGE_LINE_COLOR = Color.WHITE;
    private static final Color GRASS_COLOR = new Color(34, 139, 34);

    private static final int TRAFFIC_LIGHT_VISUAL_WIDTH = 20; // Диаметр самого сигнала
    private static final int TRAFFIC_LIGHT_POLE_HEIGHT = 30;
    private static final Color TRAFFIC_LIGHT_POLE_COLOR = Color.DARK_GRAY;
    private static final int OBJECT_SIDE_OFFSET = 5; // Отступ объекта от края дороги

    private static final int CAR_RENDER_WIDTH = 32;
    private static final int CAR_RENDER_HEIGHT = 18;
    private static final int CAR_ARC_RADIUS = 10;
    private static final Color CAR_WINDOW_COLOR = new Color(173, 216, 230, 180);

    private static final int ROAD_SIGN_SIZE = 24;
    private static final int ROAD_SIGN_POLE_HEIGHT = 30;
    private static final Color ROAD_SIGN_POLE_COLOR = Color.DARK_GRAY;
    private static final Color ROAD_SIGN_BG_COLOR = Color.WHITE;
    private static final Color ROAD_SIGN_BORDER_COLOR = Color.RED;
    private static final Color ROAD_SIGN_TEXT_COLOR = Color.BLACK;

    private final Stroke dashedStroke = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]{12, 8}, 0);
    private final Stroke solidThinStroke = new BasicStroke(2);
    private final Stroke solidThickStroke = new BasicStroke(3);


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

        int roadY = getHeight() / 2 - ROAD_RENDER_HEIGHT / 2; // Верхний Y дороги
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
                // Y верхнего края СИГНАЛА светофора
                int lightSignalTopY;
                if (light.getTargetDirection() == 1 && road.getType() != RoadType.TUNNEL && road.getNumberOfDirections() == 2) {
                    // Снизу дороги: столб начинается от нижнего края дороги и идет вниз
                    lightSignalTopY = roadY + ROAD_RENDER_HEIGHT + OBJECT_SIDE_OFFSET + TRAFFIC_LIGHT_POLE_HEIGHT;
                } else {
                    // Сверху дороги: столб начинается от верхнего края дороги и идет вверх
                    lightSignalTopY = roadY - OBJECT_SIDE_OFFSET - TRAFFIC_LIGHT_POLE_HEIGHT - TRAFFIC_LIGHT_VISUAL_WIDTH;
                }

                double roadModelLength = road.getLength();
                if (roadModelLength > 0) {
                    lightScreenX = (int) ((light.getPosition() / roadModelLength) * panelWidth);
                    lightScreenX -= TRAFFIC_LIGHT_VISUAL_WIDTH / 2; // Центрируем сам сигнал
                    lightScreenX = Math.max(0, Math.min(lightScreenX, panelWidth - TRAFFIC_LIGHT_VISUAL_WIDTH));
                } else {
                    lightScreenX = 10;
                }
                drawTrafficLight(g2d, light, lightScreenX, lightSignalTopY, roadY);
            }
        }

        if (road.getRoadSigns() != null && !road.getRoadSigns().isEmpty()){
            int panelWidth = getWidth();
            for (RoadSign sign : road.getRoadSigns()) {
                int signScreenX;
                // Y верхнего края ЗНАКА
                int signTopY;
                if (sign.getTargetDirection() == 1 && road.getNumberOfDirections() == 2 && road.getType() != RoadType.TUNNEL) {
                    // Снизу дороги
                    signTopY = roadY + ROAD_RENDER_HEIGHT + OBJECT_SIDE_OFFSET + ROAD_SIGN_POLE_HEIGHT;
                } else {
                    // Сверху дороги
                    signTopY = roadY - OBJECT_SIDE_OFFSET - ROAD_SIGN_POLE_HEIGHT - ROAD_SIGN_SIZE;
                }

                double roadModelLength = road.getLength();
                if (roadModelLength > 0) {
                    signScreenX = (int) ((sign.getPosition() / roadModelLength) * panelWidth);
                    signScreenX -= ROAD_SIGN_SIZE / 2;
                    signScreenX = Math.max(0, Math.min(signScreenX, panelWidth - ROAD_SIGN_SIZE));
                } else {
                    signScreenX = 20;
                }
                drawRoadSign(g2d, sign, signScreenX, signTopY, roadY);
            }
        }
        drawInfoPanel(g2d);
        if (placementModeActive && placementHint != null) {
            g2d.setColor(new Color(0,0,200, 200));
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            String hintText = "РЕЖИМ: " + placementHint + ". Кликните на дорогу.";
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
            g2d.setColor(CENTER_LINE_COLOR);
            g2d.setStroke(solidThickStroke);
            g2d.drawLine(0, roadY + ROAD_RENDER_HEIGHT / 2, getWidth(), roadY + ROAD_RENDER_HEIGHT / 2);
        } else if (road.getNumberOfDirections() == 1) {
            g2d.setColor(LANE_SEPARATOR_COLOR);
            g2d.setStroke(dashedStroke);
            for (int i = 1; i < numLanesTotalOnScreen; i++) {
                int lineY = roadY + i * laneVisualHeight;
                g2d.drawLine(0, lineY, getWidth(), lineY);
            }
        } else if (road.getNumberOfDirections() == 2) {
            g2d.setColor(CENTER_LINE_COLOR);
            g2d.setStroke(solidThinStroke);
            int centerLineY = roadY + lanesPerDirection * laneVisualHeight;
            int gapBetweenDoubleLines = 3;
            g2d.drawLine(0, centerLineY - gapBetweenDoubleLines / 2, getWidth(), centerLineY - gapBetweenDoubleLines / 2);
            g2d.drawLine(0, centerLineY + gapBetweenDoubleLines / 2, getWidth(), centerLineY + gapBetweenDoubleLines / 2);
            g2d.setColor(LANE_SEPARATOR_COLOR);
            g2d.setStroke(dashedStroke);
            for (int i = 1; i < lanesPerDirection; i++) {
                int lineY_dir0 = roadY + i * laneVisualHeight;
                g2d.drawLine(0, lineY_dir0, getWidth(), lineY_dir0);
                int lineY_dir1 = roadY + (lanesPerDirection * laneVisualHeight) + i * laneVisualHeight;
                g2d.drawLine(0, lineY_dir1, getWidth(), lineY_dir1);
            }
        }
        g2d.setColor(EDGE_LINE_COLOR);
        g2d.setStroke(solidThinStroke);
        g2d.drawLine(0, roadY, getWidth(), roadY);
        g2d.drawLine(0, roadY + ROAD_RENDER_HEIGHT, getWidth(), roadY + ROAD_RENDER_HEIGHT);
    }

    // screenY_signal_top - это Y координата ВЕРХНЕГО КРАЯ самого СИГНАЛА (круга)
    private void drawTrafficLight(Graphics2D g2d, TrafficLight light, int screenX, int screenY_signal_top, int roadTopY) {
        int signalDiameter = TRAFFIC_LIGHT_VISUAL_WIDTH;
        int signalRadius = signalDiameter / 2;
        g2d.setColor(TRAFFIC_LIGHT_POLE_COLOR);
        int poleX = screenX + signalRadius - 2; // Центр столба по X

        if (screenY_signal_top < roadTopY) { // Светофор СВЕРХУ дороги
            // Столб рисуется от верхнего края дороги до низа сигнала
            g2d.fillRect(poleX, roadTopY - TRAFFIC_LIGHT_POLE_HEIGHT - OBJECT_SIDE_OFFSET, 4, TRAFFIC_LIGHT_POLE_HEIGHT);
        } else { // Светофор СНИЗУ дороги
            // Столб рисуется от нижнего края дороги до верха сигнала
            g2d.fillRect(poleX, roadTopY + ROAD_RENDER_HEIGHT + OBJECT_SIDE_OFFSET, 4, TRAFFIC_LIGHT_POLE_HEIGHT);
        }

        Color signalColor;
        if (light.getCurrentState() == TrafficLightState.GREEN) signalColor = Color.GREEN.brighter();
        else if (light.getCurrentState() == TrafficLightState.RED) signalColor = Color.RED.brighter();
        else signalColor = Color.GRAY;
        g2d.setColor(signalColor);
        g2d.fillOval(screenX, screenY_signal_top, signalDiameter, signalDiameter);

        Color textColor = (light.getCurrentState() == TrafficLightState.GREEN) ? Color.BLACK : Color.WHITE;
        g2d.setColor(textColor);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(9, signalRadius - 2)));
        String timeText = String.format("%.0f", light.getRemainingTime());
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(timeText);
        g2d.drawString(timeText,
                screenX + signalRadius - textWidth / 2,
                screenY_signal_top + signalRadius + fm.getAscent() / 2 -1);
    }

    private void drawCar(Graphics2D g2d, Car car, int roadSurfaceY) {
        double roadModelLength = road.getLength();
        int panelWidth = getWidth();
        int carScreenX;

        if (roadModelLength > 0) {
            carScreenX = (int) ((car.getPosition() / roadModelLength) * panelWidth);
        } else {
            // Если длина дороги 0, машины не должны появляться, но на всякий случай
            carScreenX = (car.getDirection() == 0) ? -CAR_RENDER_WIDTH : panelWidth + CAR_RENDER_WIDTH;
        }
        carScreenX -= CAR_RENDER_WIDTH / 2; // Центрируем машину по X

        // Расчет Y координаты:
        int totalLanesOnRoad = road.getNumberOfLanes(); // Общее количество полос на дороге (например, 2+2=4 для двухсторонки по 2 полосы)
        if (totalLanesOnRoad == 0) totalLanesOnRoad = 1; // Защита от деления на ноль

        int laneVisualHeight = ROAD_RENDER_HEIGHT / totalLanesOnRoad; // Высота одной визуальной полосы на экране

        // car.getLaneIndex() - это ГЛОБАЛЬНЫЙ индекс полосы.
        // Для двухсторонней дороги с N полос в каждом направлении:
        // Направление 0 (сверху вниз на экране): полосы 0, 1, ..., N-1
        // Направление 1 (сверху вниз на экране): полосы N, N+1, ..., 2N-1
        // Этот глобальный индекс используется для смещения вниз от roadSurfaceY.
        int carScreenY = roadSurfaceY +                                  // Верхний край всей дороги
                (car.getLaneIndex() * laneVisualHeight) +        // Смещение к началу нужной полосы
                (laneVisualHeight / 2 - CAR_RENDER_HEIGHT / 2); // Центрирование машины ВНУТРИ ЕЕ полосы

        // Лог для отладки (можно оставить на время тестирования)
        // System.out.printf("DRAW CAR ID: %d, Dir: %d, LaneIdx: %d, ModelPos: %.1f -> ScreenX: %d, ScreenY: %d (RoadY: %d, PanelH: %d, LaneVisualHeight: %d)%n",
        //        car.getId(), car.getDirection(), car.getLaneIndex(), car.getPosition(),
        //        carScreenX, carScreenY, roadSurfaceY, getHeight(), laneVisualHeight);

        Color carBodyColor;
        if (car.getDirection() == 0) {
            carBodyColor = new Color(50, 90, 180); // Синий для ->
        } else {
            carBodyColor = new Color(180, 50, 90); // Красноватый для <-
        }

        Shape carShape = new RoundRectangle2D.Double(carScreenX, carScreenY, CAR_RENDER_WIDTH, CAR_RENDER_HEIGHT, CAR_ARC_RADIUS, CAR_ARC_RADIUS);
        g2d.setColor(carBodyColor);
        g2d.fill(carShape);

        g2d.setColor(CAR_WINDOW_COLOR);
        int windowWidth = CAR_RENDER_WIDTH / 2;
        int windowHeight = CAR_RENDER_HEIGHT / 2 - 2;
        int windowXOffset = (car.getDirection() == 0) ? CAR_RENDER_WIDTH / 2 - 2 : 2;
        g2d.fillRoundRect(carScreenX + windowXOffset , carScreenY + 2, windowWidth, windowHeight, CAR_ARC_RADIUS / 2, CAR_ARC_RADIUS / 2);

        g2d.setColor(carBodyColor.darker());
        g2d.draw(carShape);

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

    // screenY_sign_top - это Y координата ВЕРХНЕГО КРАЯ самого ЗНАКА
    private void drawRoadSign(Graphics2D g2d, RoadSign sign, int screenX, int screenY_sign_top, int roadTopY) {
        g2d.setColor(ROAD_SIGN_POLE_COLOR);
        int poleX = screenX + ROAD_SIGN_SIZE / 2 - 2;

        if (screenY_sign_top < roadTopY) { // Знак СВЕРХУ дороги
            // Столб от верхнего края дороги до низа знака
            g2d.fillRect(poleX, roadTopY - ROAD_SIGN_POLE_HEIGHT - OBJECT_SIDE_OFFSET, 4, ROAD_SIGN_POLE_HEIGHT);
        } else { // Знак СНИЗУ дороги
            // Столб от нижнего края дороги до верха знака
            g2d.fillRect(poleX, roadTopY + ROAD_RENDER_HEIGHT + OBJECT_SIDE_OFFSET, 4, ROAD_SIGN_POLE_HEIGHT);
        }

        if (sign.getType() == RoadSignType.SPEED_LIMIT) {
            g2d.setColor(ROAD_SIGN_BG_COLOR);
            g2d.fillOval(screenX, screenY_sign_top, ROAD_SIGN_SIZE, ROAD_SIGN_SIZE);
            g2d.setColor(ROAD_SIGN_BORDER_COLOR);
            g2d.setStroke(new BasicStroke(Math.max(1, ROAD_SIGN_SIZE / 10f)));
            g2d.drawOval(screenX, screenY_sign_top, ROAD_SIGN_SIZE, ROAD_SIGN_SIZE);
            g2d.setStroke(new BasicStroke(1));
            g2d.setColor(ROAD_SIGN_TEXT_COLOR);
            String text = String.format("%.0f", sign.getSpeedLimitKmh());
            int fontSize = ROAD_SIGN_SIZE / 2;
            if (sign.getSpeedLimitKmh() >= 100) fontSize = (int)(ROAD_SIGN_SIZE / 2.3f);
            else if (sign.getSpeedLimitKmh() < 10) fontSize = (int)(ROAD_SIGN_SIZE / 1.9f);
            g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g2d.drawString(text,
                    screenX + ROAD_SIGN_SIZE / 2 - textWidth / 2,
                    screenY_sign_top + ROAD_SIGN_SIZE / 2 + fm.getAscent() / 2 - fm.getDescent() / 3);
        } else {
            g2d.setColor(Color.BLUE);
            g2d.fillRect(screenX, screenY_sign_top, ROAD_SIGN_SIZE, ROAD_SIGN_SIZE);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, ROAD_SIGN_SIZE - 8));
            g2d.drawString("?", screenX + ROAD_SIGN_SIZE/2 - g2d.getFontMetrics().stringWidth("?")/2,
                    screenY_sign_top + ROAD_SIGN_SIZE/2 + g2d.getFontMetrics().getAscent()/2 - g2d.getFontMetrics().getDescent()/2);
        }
    }
}