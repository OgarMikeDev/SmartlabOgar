package models;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Bot extends TelegramLongPollingBot {
    Document document;
    Map<String, String> mapShares = new HashMap<>();
    StringBuilder builderShares = new StringBuilder();

    //TODO Кнопки и клавиатура для меню
    private InlineKeyboardButton buttonForListShares = InlineKeyboardButton.builder()
            .text("Получить список всех акций")
            .callbackData("список всех акций")
            .build();
    private InlineKeyboardButton buttonForPriceShare = InlineKeyboardButton.builder()
            .text("Получить цену указанной акции")
            .callbackData("цена указанной акции")
            .build();
    boolean isButtonForPriceShare = false;
    private InlineKeyboardButton buttonForNotificationMinPrice = InlineKeyboardButton.builder()
            .text("Получить желаемую цену акции для покупки")
            .callbackData("желаемая цена акции")
            .build();
    boolean isButtonForNotificationMinPrice = false;
    String nameShareForMinPrice = "";
    double priceShareForMinPrice = 0.0;
    boolean hasMinPrice = false;
    boolean foundMinPriceForShare = false;
    private InlineKeyboardButton buttonForNotificationMaxPrice = InlineKeyboardButton.builder()
            .text("Получить top цену акции для продажи")
            .callbackData("цена акции для продажи")
            .build();
    private InlineKeyboardMarkup keyboardForButtonsMenu = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(buttonForListShares))
            .keyboardRow(List.of(buttonForPriceShare))
            .keyboardRow(List.of(buttonForNotificationMinPrice))
            .keyboardRow(List.of(buttonForNotificationMaxPrice))
            .build();

    @Override
    public void onUpdateReceived(Update update) {
        forWorkWithText(update);
        forWorkWithButtons(update);
    }

    public void forWorkWithText(Update update) {
        if (update.hasMessage()) {
            String textMessage = update.getMessage().getText();
            Long idUser = update.getMessage().getFrom().getId();

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(idUser);

            System.out.println("текст: " + textMessage);
            if (textMessage.compareToIgnoreCase("/start") == 0) {
                sendMessage.setText("Меню: ");
                sendMessage.setReplyMarkup(keyboardForButtonsMenu);
            }

            if (isButtonForPriceShare && mapShares.containsKey(textMessage)) {
                sendMessage.setText("Цена " + textMessage + " составляет " + mapShares.get(textMessage) + " руб.");
                isButtonForPriceShare = false;
            }

            if (isButtonForNotificationMinPrice && mapShares.containsKey(textMessage)) {
                nameShareForMinPrice = textMessage;
                sendMessage.setText("Введите цену для акции \"" + textMessage + "\"");
                hasMinPrice = true;
            }

            else if (hasMinPrice) {
                System.out.println("Цена: " + textMessage);
                priceShareForMinPrice = Double.parseDouble(textMessage);
                sendMessage.setText("Можем парсить цену акции для покупки");
                try {
                    while (!foundMinPriceForShare) {
                        System.out.println("Ищем цену для сравнения!");
                        Document document = Jsoup.connect("https://smart-lab.ru/q/shares/").get();
                        Elements elements = document.select("tr");
                        for (Element element : elements) {
                            String strElement = element.toString();
                            if (strElement.contains(nameShareForMinPrice)) {
                                double actualPrice = Double.parseDouble(returnPrice(element));
                                if (actualPrice <= priceShareForMinPrice) {
                                    System.out.println("Покупайте \"" + nameShareForMinPrice + "\" и Вы не пожалеете :)");
                                    isButtonForNotificationMinPrice = false;
                                    hasMinPrice = false;
                                    foundMinPriceForShare = true;
                                    break;
                                }
                            }
                        }
                        TimeUnit.SECONDS.sleep(10);
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }

            try {
                execute(sendMessage);
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    public void forWorkWithButtons(Update update) {
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            try {
                document = Jsoup.connect("https://smart-lab.ru/q/shares/").get();
                fullMapNamePriceShare();
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }

            EditMessageText editMessageText = EditMessageText.builder()
                    .text("")
                    .chatId(chatId)
                    .messageId(messageId)
                    .build();

            EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .build();

            if (callbackData.equals(buttonForListShares.getCallbackData())) {
                editMessageText.setText("геракл");
                try {
                    FileWriter fileWriter = new FileWriter("src/main/resources/data/smartlab_main_page.html");
                    fileWriter.write(document.toString());

                    fileWriter.close();

                    FileWriter fileWriterForShares = new FileWriter("src/main/resources/data/name_price_share.txt");
                    fileWriterForShares.write(builderShares.toString());
                    fileWriterForShares.close();

                    SendDocument sendDocument = SendDocument.builder()
                            .document(new InputFile(new File("src/main/resources/data/name_price_share.txt")))
                            .chatId(chatId)
                            .build();

                    execute(sendDocument);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            } else if (callbackData.equals(buttonForPriceShare.getCallbackData())) {
                editMessageText.setText("Введите название акций:");
                isButtonForPriceShare = true;
            } else if (callbackData.equals(buttonForNotificationMinPrice.getCallbackData())) {
                editMessageText.setText("Введите название акций:");
                isButtonForNotificationMinPrice = true;

            }

            try {
                if (editMessageText.getText() != null) {
                    System.out.println("editMessageText: " + editMessageText);
                    execute(editMessageText);
                }
                if (editMessageReplyMarkup.getReplyMarkup() != null) {
                    System.out.println("editMessageReplyMarkup: " + editMessageReplyMarkup);
                    execute(editMessageReplyMarkup);
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    public Map<String, String> getMapShares() {
        return mapShares;
    }

    public String returnListName(Element element) {
        String name = "";
        try {
            Element elementsName = element.selectFirst(".trades-table__name");
            String strElementName = elementsName.toString();
            int leftIndexForName = strElementName.indexOf("\">", strElementName.length() / 2);
            int rightIndexForName = strElementName.indexOf("</a>", leftIndexForName);
            name = strElementName.substring(leftIndexForName + 2, rightIndexForName);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return name;
    }

    public String returnPrice(Element element) {
        String price = "";
        try {
            Element elementPrice = element.selectFirst(".trades-table__price");
            String strElementPrice = elementPrice.toString();
            int leftIndexForPrice = strElementPrice.indexOf("\">", strElementPrice.length() / 2);
            int rightIndexForPrice = strElementPrice.indexOf("</td>", leftIndexForPrice);
            price = strElementPrice.substring(leftIndexForPrice + 2, rightIndexForPrice);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return price;
    }

    public void fullMapNamePriceShare() {
        Elements elements = document.select("tr");
        for (Element element : elements) {
            String strElement = element.toString();
            if (strElement.contains("trades-table__name") &&
                    strElement.contains("trades-table__price")) {
                mapShares.put(returnListName(element), returnPrice(element));
            }
        }

        for (Map.Entry<String, String> mapShare : mapShares.entrySet()) {
            builderShares.append(mapShare.getKey() + " - " + mapShare.getValue() + " руб.\n");
        }
    }

    @Override
    public String getBotUsername() {
        return "@bot_lessonbot";
    }

    @Override
    public String getBotToken() {
        return "7822169433:AAFt2wq0qCnH3ZxTVRzLVyUpnzh6c-DbkBA";
    }
}
