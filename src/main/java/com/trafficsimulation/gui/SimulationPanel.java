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
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;

public class SimulationPanel extends JPanel {

    private Road road;
    private double simulationTime;

    private boolean placementModeActive = false;
    private String placementHint = null;

    // НОВАЯ КОНСТАНТА: Желаемая визуальная высота одной полосы
    private static final int TARGET_LANE_VISUAL_HEIGHT = 35; // px, можете подобрать это значение
    // ROAD_RENDER_HEIGHT теперь будет вычисляемой

    private static final Color ROAD_COLOR = new Color(100, 100, 100);
    private static final Color LANE_SEPARATOR_COLOR = Color.WHITE;
    private static final Color CENTER_LINE_COLOR = Color.YELLOW;
    private static final Color GRASS_COLOR = new Color(36, 102, 36);

    private static final int TRAFFIC_LIGHT_VISUAL_WIDTH = 20;
    private static final int TRAFFIC_LIGHT_POLE_HEIGHT = 30; // Высота столба над/под дорогой
    private static final Color TRAFFIC_LIGHT_POLE_COLOR = Color.DARK_GRAY;
    private static final int OBJECT_SIDE_OFFSET = 5; // Отступ столба от края дороги

    private static final int CAR_RENDER_WIDTH = 32;
    private static final int CAR_RENDER_HEIGHT = 16; // Сделаем чуть меньше, чтобы лучше помещалась на полосе
    private static final int CAR_ARC_RADIUS = 8;
    private static final Color CAR_WINDOW_COLOR = new Color(173, 216, 230, 180);
    private static final double MAX_CAR_TILT_ANGLE = Math.toRadians(5);

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
        setCursor(active ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : Cursor.getDefaultCursor());
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

        int totalLanesOnScreen = road.getNumberOfLanes();
        if (totalLanesOnScreen == 0) totalLanesOnScreen = 1; // Минимум одна полоса для расчетов

        // Динамически вычисляемая общая высота дороги на экране
        int currentRoadRenderHeight = TARGET_LANE_VISUAL_HEIGHT * totalLanesOnScreen;
        // Y-координата верхнего края дороги, центрируем дорогу
        int roadVisualTopY = getHeight() / 2 - currentRoadRenderHeight / 2;

        drawRoadSurfaceAndMarkings(g2d, roadVisualTopY, currentRoadRenderHeight);

        if (road.getCars() != null) {
            for (Car car : road.getCars()) {
                // Передаем TARGET_LANE_VISUAL_HEIGHT вместо currentRoadRenderHeight / totalLanesOnScreen
                drawCar(g2d, car, roadVisualTopY, TARGET_LANE_VISUAL_HEIGHT);
            }
        }
        drawRoadObjects(g2d, roadVisualTopY, currentRoadRenderHeight);
        drawInfoPanel(g2d);
        drawPlacementHint(g2d, currentRoadRenderHeight); // Передаем высоту для корректного позиционирования
    }

    private void drawRoadSurfaceAndMarkings(Graphics2D g2d, int roadVisualTopY, int currentRoadRenderHeight) {
        g2d.setColor(ROAD_COLOR);
        g2d.fillRect(0, roadVisualTopY, getWidth(), currentRoadRenderHeight);

        int lanesPerModelDir = road.getLanesPerDirection();
        if (lanesPerModelDir == 0) lanesPerModelDir = 1;

        // Визуальная высота одной полосы теперь TARGET_LANE_VISUAL_HEIGHT
        int laneVisualHeight = TARGET_LANE_VISUAL_HEIGHT;

        g2d.setColor(Color.WHITE);
        g2d.setStroke(solidThinStroke);
        g2d.drawLine(0, roadVisualTopY, getWidth(), roadVisualTopY);
        g2d.drawLine(0, roadVisualTopY + currentRoadRenderHeight, getWidth(), roadVisualTopY + currentRoadRenderHeight);

        if (road.getType() == RoadType.TUNNEL) {
            g2d.setColor(CENTER_LINE_COLOR);
            g2d.setStroke(solidThickStroke);
            g2d.drawLine(0, roadVisualTopY + currentRoadRenderHeight / 2, getWidth(), roadVisualTopY + currentRoadRenderHeight / 2);
        } else if (road.getNumberOfDirections() == 1) {
            g2d.setColor(LANE_SEPARATOR_COLOR);
            g2d.setStroke(dashedStroke);
            for (int i = 1; i < lanesPerModelDir; i++) { // lanesPerModelDir здесь == общему числу полос
                g2d.drawLine(0, roadVisualTopY + i * laneVisualHeight, getWidth(), roadVisualTopY + i * laneVisualHeight);
            }
        } else if (road.getNumberOfDirections() == 2) {
            g2d.setColor(CENTER_LINE_COLOR);
            g2d.setStroke(solidThinStroke);
            int centerLineY = roadVisualTopY + lanesPerModelDir * laneVisualHeight;
            int gap = 3;
            g2d.drawLine(0, centerLineY - gap / 2, getWidth(), centerLineY - gap / 2);
            g2d.drawLine(0, centerLineY + gap / 2, getWidth(), centerLineY + gap / 2);
            g2d.setColor(LANE_SEPARATOR_COLOR);
            g2d.setStroke(dashedStroke);
            for (int i = 1; i < lanesPerModelDir; i++) {
                g2d.drawLine(0, roadVisualTopY + i * laneVisualHeight, getWidth(), roadVisualTopY + i * laneVisualHeight);
                g2d.drawLine(0, roadVisualTopY + lanesPerModelDir * laneVisualHeight + i * laneVisualHeight, getWidth(), roadVisualTopY + lanesPerModelDir * laneVisualHeight + i * laneVisualHeight);
            }
        }
    }

    private void drawRoadObjects(Graphics2D g2d, int roadVisualTopY, int currentRoadRenderHeight) {
        int panelWidth = getWidth();
        if (road.getTrafficLights() != null) {
            for (TrafficLight light : road.getTrafficLights()) {
                int lightModelDir = light.getTargetDirection();
                boolean placeAbove = (road.getType() == RoadType.TUNNEL) ? (lightModelDir == 1) : (lightModelDir == 1 || lightModelDir == -1);

                int lightSignalTopY = placeAbove ?
                        roadVisualTopY - OBJECT_SIDE_OFFSET - TRAFFIC_LIGHT_POLE_HEIGHT - TRAFFIC_LIGHT_VISUAL_WIDTH :
                        roadVisualTopY + currentRoadRenderHeight + OBJECT_SIDE_OFFSET + TRAFFIC_LIGHT_POLE_HEIGHT; // Используем currentRoadRenderHeight

                int lightScreenX = (int) ((light.getPosition() / road.getLength()) * panelWidth) - TRAFFIC_LIGHT_VISUAL_WIDTH / 2;
                lightScreenX = Math.max(0, Math.min(lightScreenX, panelWidth - TRAFFIC_LIGHT_VISUAL_WIDTH));
                drawTrafficLight(g2d, light, lightScreenX, lightSignalTopY, roadVisualTopY, currentRoadRenderHeight, placeAbove);
            }
        }

        if (road.getRoadSigns() != null) {
            for (RoadSign sign : road.getRoadSigns()) {
                int signModelDir = sign.getTargetDirection();
                boolean primaryPlaceAbove = (road.getType() == RoadType.TUNNEL) || (signModelDir == 1) || (signModelDir == -1 && road.getNumberOfDirections() == 2);

                int signTopY = primaryPlaceAbove ?
                        roadVisualTopY - OBJECT_SIDE_OFFSET - ROAD_SIGN_POLE_HEIGHT - ROAD_SIGN_SIZE :
                        roadVisualTopY + currentRoadRenderHeight + OBJECT_SIDE_OFFSET + ROAD_SIGN_POLE_HEIGHT; // Используем currentRoadRenderHeight

                int signScreenX = (int) ((sign.getPosition() / road.getLength()) * panelWidth) - ROAD_SIGN_SIZE / 2;
                signScreenX = Math.max(0, Math.min(signScreenX, panelWidth - ROAD_SIGN_SIZE));
                drawRoadSign(g2d, sign, signScreenX, signTopY, roadVisualTopY, currentRoadRenderHeight, primaryPlaceAbove);

                if (signModelDir == -1 && road.getNumberOfDirections() == 2 && primaryPlaceAbove) {
                    int bottomSignTopY = roadVisualTopY + currentRoadRenderHeight + OBJECT_SIDE_OFFSET + ROAD_SIGN_POLE_HEIGHT;
                    drawRoadSign(g2d, sign, signScreenX, bottomSignTopY, roadVisualTopY, currentRoadRenderHeight, false);
                }
            }
        }
    }

    private void drawTrafficLight(Graphics2D g2d, TrafficLight light, int screenX, int screenY_signal_top, int roadVisualTopY, int currentRoadRenderHeight, boolean isAboveRoad) {
        int signalDiameter = TRAFFIC_LIGHT_VISUAL_WIDTH;
        g2d.setColor(TRAFFIC_LIGHT_POLE_COLOR);
        int poleX = screenX + signalDiameter / 2 - 2;
        if (isAboveRoad) g2d.fillRect(poleX, roadVisualTopY - TRAFFIC_LIGHT_POLE_HEIGHT - OBJECT_SIDE_OFFSET, 4, TRAFFIC_LIGHT_POLE_HEIGHT);
        else g2d.fillRect(poleX, roadVisualTopY + currentRoadRenderHeight + OBJECT_SIDE_OFFSET, 4, TRAFFIC_LIGHT_POLE_HEIGHT); // Используем currentRoadRenderHeight

        g2d.setColor(light.getCurrentState() == TrafficLightState.GREEN ? Color.GREEN.brighter() : (light.getCurrentState() == TrafficLightState.RED ? Color.RED.brighter() : Color.GRAY));
        g2d.fillOval(screenX, screenY_signal_top, signalDiameter, signalDiameter);
        g2d.setColor(light.getCurrentState() == TrafficLightState.GREEN ? Color.BLACK : Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(9, signalDiameter / 2 - 2)));
        String timeText = String.format("%.0f", light.getRemainingTime());
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(timeText, screenX + signalDiameter / 2 - fm.stringWidth(timeText) / 2, screenY_signal_top + signalDiameter / 2 + fm.getAscent() / 2 - 1);
    }

    // laneVisualHeight теперь TARGET_LANE_VISUAL_HEIGHT
    private void drawCar(Graphics2D g2d, Car car, int roadVisualTopY, int laneVisualHeight) {
        int panelWidth = getWidth();
        int carScreenX = (int) ((car.getPosition() / road.getLength()) * panelWidth) - CAR_RENDER_WIDTH / 2;

        int globalLaneForDrawing = road.getGlobalLaneIndexForDrawing(car.getCurrentLaneIndex(), car.getDirection());
        if (globalLaneForDrawing == -1) return;

        double yOffsetForLaneChange = 0;
        double rotationForLaneChange = 0;

        if (car.isChangingLane()) {
            int globalTargetLaneForDrawing = road.getGlobalLaneIndexForDrawing(car.getTargetLaneForChange(), car.getDirection());
            if (globalTargetLaneForDrawing != -1) {
                double progress = car.getLaneChangeProgress();
                yOffsetForLaneChange = (globalTargetLaneForDrawing - globalLaneForDrawing) * laneVisualHeight * progress;
                double normalizedProgress = (progress < 0.5) ? (progress * 2.0) : ((1.0 - progress) * 2.0);
                int changeDirectionSign = (globalTargetLaneForDrawing > globalLaneForDrawing) ? 1 : -1;
                rotationForLaneChange = changeDirectionSign * MAX_CAR_TILT_ANGLE * Math.sin(normalizedProgress * Math.PI);
            }
        }

        int carCenterYOnLane = roadVisualTopY + (globalLaneForDrawing * laneVisualHeight) + laneVisualHeight / 2;
        int carTopY = carCenterYOnLane - CAR_RENDER_HEIGHT / 2 + (int)yOffsetForLaneChange;

        AffineTransform oldTransform = g2d.getTransform();
        g2d.rotate(rotationForLaneChange, carScreenX + CAR_RENDER_WIDTH / 2.0, carTopY + CAR_RENDER_HEIGHT / 2.0);

        Color carBodyColor = (car.getDirection() == 1) ? new Color(50, 90, 180) : new Color(180, 50, 90);
        Shape carShape = new RoundRectangle2D.Double(carScreenX, carTopY, CAR_RENDER_WIDTH, CAR_RENDER_HEIGHT, CAR_ARC_RADIUS, CAR_ARC_RADIUS);
        g2d.setColor(carBodyColor);
        g2d.fill(carShape);
        g2d.setColor(CAR_WINDOW_COLOR);
        int windowXOffset = (car.getDirection() == 0) ? CAR_RENDER_WIDTH / 2 - 2 : 2;
        g2d.fillRoundRect(carScreenX + windowXOffset, carTopY + 2, CAR_RENDER_WIDTH / 2, CAR_RENDER_HEIGHT / 2 - 2, CAR_ARC_RADIUS / 2, CAR_ARC_RADIUS / 2);
        g2d.setColor(carBodyColor.darker());
        g2d.draw(carShape);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        String speedText = String.format("%.0f", car.getCurrentSpeed() * 3.6);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(speedText, carScreenX + CAR_RENDER_WIDTH / 2 - fm.stringWidth(speedText) / 2, carTopY + CAR_RENDER_HEIGHT / 2 + fm.getAscent() / 2 - 2);

        g2d.setTransform(oldTransform);
    }

    private void drawInfoPanel(Graphics2D g2d) {
        // ... (без изменений) ...
        g2d.setColor(new Color(0,0,0,150));
        g2d.fillRect(5,5, 250, 50);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString(String.format("Время: %.1f c", simulationTime), 10, 20);
        g2d.drawString("Машин: " + (road.getCars() != null ? road.getCars().size() : 0), 10, 35);
        String roadInfo = "Дорога: " + road.getType() + ", " +
                (road.getNumberOfDirections() == 1 ? "1-стор." : "2-стор.") + ", " +
                road.getLanesPerDirection() + " п./напр.";
        if(road.getType() == RoadType.TUNNEL) roadInfo = "Дорога: " + road.getType() + " (реверс.)";
        g2d.drawString(roadInfo, 10, 50);
    }

    private void drawPlacementHint(Graphics2D g2d, int currentRoadRenderHeight) { // Добавлен параметр
        if (placementModeActive && placementHint != null) {
            g2d.setColor(new Color(0,0,200, 200));
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            String text = "РЕЖИМ: " + placementHint + ". Кликните на дорогу.";
            FontMetrics fm = g2d.getFontMetrics();
            int w = fm.stringWidth(text);
            // Позиционируем подсказку чуть ниже общей информации или внизу, если дорога очень высокая
            int hintY = 65 + fm.getAscent(); // Отступ от верха панели
            if (getHeight() - (getHeight()/2 + currentRoadRenderHeight/2) < 80) { // Если мало места снизу
                hintY = getHeight() - 30; // Прижимаем к низу
            } else {
                hintY = getHeight()/2 + currentRoadRenderHeight/2 + 30; // Под дорогой
            }

            g2d.fillRect(getWidth() / 2 - w / 2 - 10, hintY - fm.getAscent(), w + 20, fm.getHeight() + 5);
            g2d.setColor(Color.WHITE);
            g2d.drawString(text, getWidth() / 2 - w / 2, hintY);
        }
    }

    private void drawRoadSign(Graphics2D g2d, RoadSign sign, int screenX, int screenY_sign_top, int roadVisualTopY, int currentRoadRenderHeight, boolean isAboveRoad) {
        g2d.setColor(ROAD_SIGN_POLE_COLOR);
        int poleX = screenX + ROAD_SIGN_SIZE / 2 - 2;
        if (isAboveRoad) g2d.fillRect(poleX, roadVisualTopY - ROAD_SIGN_POLE_HEIGHT - OBJECT_SIDE_OFFSET, 4, ROAD_SIGN_POLE_HEIGHT);
        else g2d.fillRect(poleX, roadVisualTopY + currentRoadRenderHeight + OBJECT_SIDE_OFFSET, 4, ROAD_SIGN_POLE_HEIGHT); // Используем currentRoadRenderHeight

        if (sign.getType() == RoadSignType.SPEED_LIMIT) {
            g2d.setColor(ROAD_SIGN_BG_COLOR);
            g2d.fillOval(screenX, screenY_sign_top, ROAD_SIGN_SIZE, ROAD_SIGN_SIZE);
            g2d.setColor(ROAD_SIGN_BORDER_COLOR);
            g2d.setStroke(new BasicStroke(Math.max(1, ROAD_SIGN_SIZE / 10f)));
            g2d.drawOval(screenX, screenY_sign_top, ROAD_SIGN_SIZE, ROAD_SIGN_SIZE);
            g2d.setStroke(new BasicStroke(1));
            g2d.setColor(ROAD_SIGN_TEXT_COLOR);
            String text = String.format("%.0f", sign.getSpeedLimitKmh());
            int fontSize = (sign.getSpeedLimitKmh() >= 100) ? (int)(ROAD_SIGN_SIZE / 2.3f) : ((sign.getSpeedLimitKmh() < 10) ? (int)(ROAD_SIGN_SIZE / 1.9f) : ROAD_SIGN_SIZE / 2);
            g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(text, screenX + ROAD_SIGN_SIZE / 2 - fm.stringWidth(text) / 2, screenY_sign_top + ROAD_SIGN_SIZE / 2 + fm.getAscent() / 2 - fm.getDescent() / 3);
        } else {
            g2d.setColor(Color.BLUE); g2d.fillRect(screenX, screenY_sign_top, ROAD_SIGN_SIZE, ROAD_SIGN_SIZE);
            g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.BOLD, ROAD_SIGN_SIZE - 8));
            g2d.drawString("?", screenX + ROAD_SIGN_SIZE/2 - g2d.getFontMetrics().stringWidth("?")/2, screenY_sign_top + ROAD_SIGN_SIZE/2 + g2d.getFontMetrics().getAscent()/2 - g2d.getFontMetrics().getDescent()/2);
        }
    }
}