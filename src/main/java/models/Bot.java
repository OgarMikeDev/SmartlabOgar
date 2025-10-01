package models;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
    //TODO Кнопки и клавиатура для меню
    private InlineKeyboardButton buttonForListShares = InlineKeyboardButton.builder()
            .text("Получить список всех акций")
            .callbackData("список всех акций")
            .build();
    private InlineKeyboardButton buttonForPriceShare = InlineKeyboardButton.builder()
            .text("Получить цену указанной акции")
            .callbackData("цена указанной акции")
            .build();
    private InlineKeyboardButton buttonForNotificationMinPrice = InlineKeyboardButton.builder()
            .text("Получить желаемую цену акции")
            .callbackData("желаемая цена акции")
            .build();
    private InlineKeyboardButton buttonForNotificationMaxPrice = InlineKeyboardButton.builder()
            .text("Получить цену акции для продажи")
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

            if (textMessage.compareToIgnoreCase("/start") == 0) {
                Long idUser = update.getMessage().getFrom().getId();

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(idUser);
                sendMessage.setText("Меню: ");
                sendMessage.setReplyMarkup(keyboardForButtonsMenu);

                try {
                    execute(sendMessage);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
    }

    public void forWorkWithButtons(Update update) {
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

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
                editMessageText.setText("asdfasf");
                try {
                    Document document = Jsoup.connect("https://smart-lab.ru/q/shares/").get();

                    FileWriter fileWriter = new FileWriter("src/main/resources/data/smartlab_main_page.html");
                    fileWriter.write(document.toString());

                    fileWriter.close();
                    Elements elements = document.select("tr");
                    for (Element element : elements) {
                        String strElement = element.toString();
                        if (strElement.contains("trades-table__name")) {
                            System.out.println("называние " + returnListName(element));
                        }
                        if (strElement.contains("trades-table__price")) {
                            System.out.println("цена " + returnListPrice(element));
                        }
                    }

                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
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

    public String returnListPrice(Element element) {
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

    @Override
    public String getBotUsername() {
        return "@bot_lessonbot";
    }

    @Override
    public String getBotToken() {
        return "7822169433:AAFt2wq0qCnH3ZxTVRzLVyUpnzh6c-DbkBA";
    }
}
