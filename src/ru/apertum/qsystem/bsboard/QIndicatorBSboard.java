/*
 *  Copyright (C) 2010 {Apertum}Projects. web: www.apertum.ru email: info@apertum.ru
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ru.apertum.qsystem.bsboard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import ru.apertum.qsystem.client.forms.AFBoardRedactor;
import ru.apertum.qsystem.client.forms.FBoardConfig;
import ru.apertum.qsystem.common.CustomerState;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.server.QServer;
import ru.apertum.qsystem.server.controller.IIndicatorBoard;
import ru.apertum.qsystem.server.model.QPlanService;
import ru.apertum.qsystem.server.model.QService;
import ru.apertum.qsystem.server.model.QServiceTree;
import ru.apertum.qsystem.server.model.QUser;
import ru.apertum.qsystem.server.model.QUserList;

/**
 * Вывод информации на мониторы. Класс-менеджер вывода информации на общее табло в виде монитора.
 *
 * @author Evgeniy Egorov
 */
public class QIndicatorBSboard implements IIndicatorBoard {

    protected Fbs indicatorBoard = null;
    protected String configFile;

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        final String err = ("/".equals(File.separator)) ? "\\" : "/";
        while (configFile.indexOf(err) != -1) {
            configFile = configFile.replace(err, File.separator);
        }
        this.configFile = configFile;
    }

    private String template;
    static public final String filePath = "config/bs.html";

    private String prepareContent(String cnt) {
        String s = cnt;

        final ArrayList<String> invList = new ArrayList<>();
        int i = 0;
        for (QUser user : QUserList.getInstance().getItems()) {
            final String id = AddrProp.getInstance().getId(user.getPoint());
            if (user.getCustomer() != null) {
                if (user.getCustomer().getState() == CustomerState.STATE_INVITED || user.getCustomer().getState() == CustomerState.STATE_INVITED_SECONDARY) {
                    invList.add(id);
                    String string = "\\[" + id + "\\|blink\\]";
                    s = s.replaceAll(string, "blink_" + i++);

                    string = "\\[" + id + "\\|1\\]";
                    s = s.replaceAll(string, user.getCustomer().getFullNumber());
                }
                String string = "\\[" + id + "\\|name\\]";
                s = s.replaceAll(string, user.getCustomer().getService().getName());

                string = "\\[" + id + "\\|discription\\]";
                s = s.replaceAll(string, user.getCustomer().getService().getDescription());

                string = "\\[" + id + "\\|user\\]";
                s = s.replaceAll(string, user.getName());

                string = "\\[" + id + "\\|ext\\]";
                s = s.replaceAll(string, user.getPointExt());
            }
            String string = "\\[" + id + "\\|point\\]";
            s = s.replaceAll(string, user.getPoint());
        }
        s = s.replaceAll("\\[\\d+\\|(name|discription|point|blink|ext|user)\\]", "");

        ArrayList<String> allMatches = new ArrayList<>();
        Matcher m = Pattern.compile("\\[\\d+\\|\\d+\\]").matcher(s);
        while (m.find()) {
            allMatches.add(m.group());
        }
        for (String string : allMatches) {
            final String id = string.substring(1, string.indexOf("|"));
            int pos = Integer.parseInt(string.substring(string.indexOf("|") + 1, string.length() - 1)) - (invList.contains(id) ? 1 : 0);
            final String adr = AddrProp.getInstance().getAddr(id);
            QUser usr = null;
            for (QUser user : QUserList.getInstance().getItems()) {
                if (user.getPoint().equalsIgnoreCase(adr)) {
                    if (usr == null) {
                        usr = user;
                    } else if (user.getShadow() != null) {
                        usr = user;
                    }
                }
            }
            if (usr == null) {
                s = s.replaceAll(string.replace("[", "\\[").replace("]", "\\]").replace("|", "\\|"), "");
            } else {
                final QService ss = new QService();
                for (QPlanService pser : usr.getPlanServices()) {
                    QService ser = QServiceTree.getInstance().getById(pser.getService().getId());
                    for (QCustomer c : ser.getClients()) {
                        ss.addCustomer(c);
                    }
                }
                // для получения правильной очередности хвоста
                final PriorityQueue<QCustomer> custs = new PriorityQueue<>();
                final LinkedList<QCustomer> qeue = new LinkedList<>();
                for (QCustomer qCustomer : ss.getClients()) {
                    custs.offer(qCustomer);
                }
                while (custs.size()>0){
                    qeue.add(custs.poll());
                }
                // замена
                s = s.replaceAll(string.replace("[", "\\[").replace("]", "\\]").replace("|", "\\|"), ss.getClients().size() >= pos ? qeue.get(pos - 1).getFullNumber() : "");
            }
        }
        return s;
    }

    /**
     * Создадим форму, спозиционируем, сконфигурируем и покажем
     *
     */
    protected void initIndicatorBoard() {
        final File conff = new File(filePath);
        if (conff.exists()) {
            template = "";
            try (FileInputStream fis = new FileInputStream(conff); Scanner s = new Scanner(new InputStreamReader(fis, "UTF-8"))) {
                while (s.hasNextLine()) {
                    final String line = s.nextLine().trim();
                    template += line;
                }
            } catch (IOException ex) {
                System.err.println(ex);
                throw new RuntimeException(ex);
            }

        } else {
            throw new ServerException("Не найден " + filePath, new FileNotFoundException(filePath));
        }

        if (indicatorBoard == null) {
            indicatorBoard = new Fbs();
            if (indicatorBoard == null) {
                QLog.l().logger().warn("Табло не демонстрируется. Отключено в настройках.");
                return;
            }
            try {
                indicatorBoard.setIconImage(ImageIO.read(QServer.class.getResource("/ru/apertum/qsystem/client/forms/resources/recent.png")));
            } catch (IOException ex) {
                System.err.println(ex);
            }
            // Определим форму нв монитор
            indicatorBoard.toPosition(QLog.l().isDebug(), 20, 20);

            indicatorBoard.loadContent(prepareContent(template));

            java.awt.EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    indicatorBoard.setVisible(true);
                }
            });
        } else {
            indicatorBoard.loadContent(prepareContent(template));
        }
    }

    public QIndicatorBSboard() {
        QLog.l().logger().info("Создание табло для телевизоров или мониторов.");
    }

    /**
     * Переопределено что бы вызвать появление таблички с номером вызванного поверх главного табло
     *
     * @param user
     * @param customer
     */
    @Override
    public synchronized void inviteCustomer(QUser user, QCustomer customer) {
        QLog.l().logger().trace("Приглшием кастомера на BS табло");
        if (indicatorBoard != null) {
            indicatorBoard.loadContent(prepareContent(template));
        }
    }

    @Override
    public Element getConfig() {
        return new DOMElement("fuckYou");
    }

    @Override
    public void saveConfig(Element element) {
        // в темповый файл
        final FileOutputStream fos;
        try {
            fos = new FileOutputStream(getConfigFile());
        } catch (FileNotFoundException ex) {
            throw new ServerException("Не возможно создать файл конфигурации главного табло. " + ex.getMessage());
        }
        try {
            fos.write(element.asXML().getBytes("UTF-8"));
            fos.flush();
            fos.close();
        } catch (IOException ex) {
            throw new ServerException("Не возможно сохранить изменения в поток при сохранении файла конфигурации главного табло." + ex.getMessage());
        }
    }

    @Override
    public AFBoardRedactor getRedactor() {
        if (boardConfig == null) {
            boardConfig = FBoardConfig.getBoardConfig(null, false);
        }
        return boardConfig;
    }
    /**
     * Используемая ссылка на диалоговое окно. Singleton
     */
    private static FBoardConfig boardConfig;

    @Override
    public void showBoard() {
        QLog.l().logger().trace("Показываем BS табло");
        initIndicatorBoard();
    }

    /**
     * Выключить информационное табло.
     */
    @Override
    public synchronized void close() {
        QLog.l().logger().trace("Закрываем BS табло");
        if (indicatorBoard != null) {
            indicatorBoard.setVisible(false);
            indicatorBoard = null;
        }
    }

    @Override
    public void refresh() {
        QLog.l().logger().trace("Обновляем BS табло");
        close();
        indicatorBoard = null;
        initIndicatorBoard();
    }

    @Override
    public void clear() {

    }

    @Override
    public String getDescription() {
        return "Плагин табло со стационарными позициями.";
    }

    @Override
    public long getUID() {
        return 2;
    }

    @Override
    public void workCustomer(QUser user) {
        QLog.l().logger().trace("Работа с кастомером на BS табло");
        if (indicatorBoard != null) {
            indicatorBoard.loadContent(prepareContent(template));
        }
    }

    @Override
    public void killCustomer(QUser user) {
        QLog.l().logger().trace("Убиоаем кастомера на BS табло");
        if (indicatorBoard != null) {
            indicatorBoard.loadContent(prepareContent(template));
        }
    }

}
