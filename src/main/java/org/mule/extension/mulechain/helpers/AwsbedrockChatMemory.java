package org.mule.extension.mulechain.helpers;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AwsbedrockChatMemory {
    private MVStore store;
    private MVMap<Long, String> chatMap;

    public AwsbedrockChatMemory(String dbFile, String memoryName) {
        // Open or create the MVStore file
        store = MVStore.open(dbFile);
        
        // Create or retrieve the chat map
        chatMap = store.openMap(memoryName);
    }

    public void addMessage(long messageId, String messageContent) {
        chatMap.put(messageId, messageContent);
        store.commit(); // Save changes
    }

    public void deleteMessage(long messageId) {
        chatMap.remove(messageId);
        store.commit(); // Save changes
    }

    public void deleteAllMessages() {
        chatMap.clear();
        store.commit(); // Save changes
    }

    public String getMessage(long messageId) {
        return chatMap.get(messageId);
    }

    public int getMessageCount() {
        return chatMap.size();
    }

    public List<String> getAllMessages() {
        return new ArrayList<>(chatMap.values());
    }

    public List<String> getAllMessagesByMessageIdDesc() {
        // Retrieve all messageIds and sort them in descending order
        List<Long> messageIds = new ArrayList<>(chatMap.keySet());
        messageIds.sort(Comparator.reverseOrder());

        // Retrieve messages in descending order of messageId
        List<String> messages = new ArrayList<>();
        for (long messageId : messageIds) {
            messages.add(chatMap.get(messageId));
        }
        return messages;
    }

    public List<String> getAllMessagesByMessageIdAsc() {
        // Retrieve all messageIds and sort them in ascending order
        List<Long> messageIds = new ArrayList<>(chatMap.keySet());
        messageIds.sort(Comparator.naturalOrder());

        // Retrieve messages in ascending order of messageId
        List<String> messages = new ArrayList<>();
        for (long messageId : messageIds) {
            messages.add(chatMap.get(messageId));
        }
        return messages;
    }

    public void close() {
        store.close();
    }
}