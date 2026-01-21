package org.mule.extension.bedrock.internal.helper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

public class BedrockChatMemory implements AutoCloseable {

  private final MVStore store;
  private final MVMap<Long, String> chatMap;

  public BedrockChatMemory(String dbFile, String memoryName) {
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
    return chatMap.keySet().stream()
        .sorted(Comparator.reverseOrder())
        .map(chatMap::get)
        .collect(Collectors.toList());
  }

  public List<String> getAllMessagesByMessageIdAsc() {
    return chatMap.keySet().stream()
        .sorted(Comparator.naturalOrder())
        .map(chatMap::get)
        .collect(Collectors.toList());
  }

  @Override
  public void close() {
    store.close();
  }
}
