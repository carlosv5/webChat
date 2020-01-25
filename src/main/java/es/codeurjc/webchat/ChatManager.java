package es.codeurjc.webchat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.tomcat.jni.Time;

public class ChatManager {

	private Map<String, Chat> chats = new ConcurrentHashMap<>();
	private Map<String, User> users = new ConcurrentHashMap<>();
	private int maxChats;

	public ChatManager(int maxChats) {
		this.maxChats = maxChats;
	}

	public void newUser(User user) {
		
		if(users.containsKey(user.getName())){
			throw new IllegalArgumentException("There is already a user with name \'"
					+ user.getName() + "\'");
		} else {
			users.putIfAbsent(user.getName(), user);
		}
	}

	public Chat newChat(String name, long timeout, TimeUnit unit) throws InterruptedException,
			TimeoutException {

		if (chats.size() == maxChats) {
			Boolean[] noCapacityForMoreChats = new Boolean[1];

			Thread t = new Thread(() -> {
				long start = System.currentTimeMillis();
				long end = start + 100000000; //Fix time as parameter
				noCapacityForMoreChats[0] = true;
				while(System.currentTimeMillis() < end) {
					noCapacityForMoreChats[0] = chats.size() == maxChats;
					System.out.println("bucle");
					if(!noCapacityForMoreChats[0]) {
						System.out.println("break");
						break;
					}
				}
			});

			t.start();
			t.join();
			if(noCapacityForMoreChats[0]) {
				throw new TimeoutException("There is no enought capacity to create a new chat");
			}
		}

		if(chats.containsKey(name)){
			return chats.get(name);
		} else {
			Chat newChat = new Chat(this, name);
			chats.putIfAbsent(name, newChat);
			
			List<Thread> threads = new ArrayList<Thread>(users.size());
			for(User user : users.values()){
				Thread t = new Thread(() -> {
					user.newChat(newChat);
				});
				threads.add(t);
			}
			for(Thread th : threads) {
				th.start();
			}
			
			for(Thread th : threads ) {
				th.join();
			}
			return newChat;
		}
	}

	public void closeChat(Chat chat) {
		Chat removedChat = chats.remove(chat.getName());
		if (removedChat == null) {
			throw new IllegalArgumentException("Trying to remove an unknown chat with name \'"
					+ chat.getName() + "\'");
		}

		List<Thread> threads = new ArrayList<Thread>(users.size());
		for(User user : users.values()){
			Thread t = new Thread(() -> {
				user.chatClosed(removedChat);
			});
			threads.add(t);
		}
		for(Thread th : threads) {
			th.start();
		}
		for(Thread th : threads) {
			try {
				th.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public Collection<Chat> getChats() {
		return Collections.unmodifiableCollection(chats.values());
	}

	public Chat getChat(String chatName) {
		return chats.get(chatName);
	}

	public Collection<User> getUsers() {
		return Collections.unmodifiableCollection(users.values());
	}

	public User getUser(String userName) {
		return users.get(userName);
	}

	public boolean existChat(String chatName) {
		return chats.get(chatName) != null;

	}

	public void close() {}
}
