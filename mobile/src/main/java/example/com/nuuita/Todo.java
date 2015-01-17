package example.com.nuuita;

import android.util.Log;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.UUID;

@ParseClassName("Todo")
public class Todo extends ParseObject {

    public static String AUTHOR_KEY = "author";
    public static String IS_DRAFT_KEY = "isDraft";
    public static String UUID_KEY = "uuid";
    public static String TITLE_KEY = "title";
    public static String LIST_NAME_KEY = "todoListName";
	
	public String getTitle() {
		return getString(TITLE_KEY);
	}
	
	public void setTitle(String title) {
        String titleN = new String(title);
        Log.d("setTitle", "Old title : " + getTitle() + " new title : " + title + " UUID : "+ getUuidString());
		put(TITLE_KEY, titleN);
	}
	
	public ParseUser getAuthor() {
		return getParseUser(AUTHOR_KEY);
	}
	
	public void setAuthor(ParseUser currentUser) {
		put(AUTHOR_KEY, currentUser);
	}
	
	public boolean isDraft() {
		return getBoolean(IS_DRAFT_KEY);
	}
	
	public void setDraft(boolean isDraft) {
		put(IS_DRAFT_KEY, isDraft);
	}
	
	public void setUuidString() {
	    UUID uuid = UUID.randomUUID();
	    put("uuid", uuid.toString());
	}
	
	public String getUuidString() {
		return getString(UUID_KEY);
	}
	
	public static ParseQuery<Todo> getQuery() {
		return ParseQuery.getQuery(Todo.class);
	}

    public String getTodoListName() {
        return getString(LIST_NAME_KEY);
    }

    public void setTodoListName(String todoListName) {
        put(LIST_NAME_KEY, todoListName);
    }
}
