package example.com.nuuita;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.ParseACL;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseRole;
import com.parse.ParseUser;

import java.util.List;

/**
 * Created by Arnaud Rover on 26/12/14.
 */
public class TodoListFragment extends Fragment {

    // Adapter for the Todos Parse Query
    private TodoListAdapter todoListAdapter;
    private List<Todo> todoList;
    public static final String TAG = "TodoListFragment";

    // For showing empty and non-empty todo views
    private Button buttonOk;
    private String todoListName;
    private ParseRole todoListRole;

    private TextView todolistTitleInfoView;

    public static TodoListFragment newInstance(ParseRole role) {
        TodoListFragment fragment = new TodoListFragment();
        fragment.setTodoListRole(role);
        fragment.setTodoListName(role.getString(Todo.LIST_NAME_KEY));
        fragment.getTodos(true);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle SavedInstanceState) {

        super.onCreate(SavedInstanceState);
        setHasOptionsMenu(true);
        View v = inflater.inflate(R.layout.todo_list_fragment, parent, false);

        // If User is not Logged In Start Activity Login
        if (ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
            Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
            startActivity(loginIntent);
        }

        // Set up the views
        todolistTitleInfoView = (TextView) v.findViewById(R.id.todolist_title);
        buttonOk = (Button) v.findViewById(R.id.buttonSetTODO);
        buttonOk.setVisibility(View.INVISIBLE);
        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                synchroniseTodos();
            }
        });
        updateListInfo();

        List<Todo> todoList = getTodos(false);
        //Add empty item at the end of the list
        Todo emptyTodo = new Todo();
        initEmptyTodo(emptyTodo);
        todoListAdapter = new TodoListAdapter(getActivity(), R.layout.list_item_todo, todoList, buttonOk, this);
        todoListAdapter.addItem(emptyTodo);
        // Attach the query adapter to the view
        ListView todoListView = (ListView) v.findViewById(R.id.todo_list_view);
        todoListView.setAdapter(todoListAdapter);

        return v;
    }

    private List<Todo> getTodos(boolean forceServerRequest) {
        List<Todo> requestedTodoList = todoList;

        if (forceServerRequest == true) {
            ParseQuery<Todo> query = Todo.getQuery();
            query.whereEqualTo(Todo.LIST_NAME_KEY, todoListName);
            try {
                requestedTodoList = query.find();
                Log.d("Load " , todoListName + ": size=" + requestedTodoList.size());
                todoList = requestedTodoList;
                for (int i = 0; i < todoList.size(); i++) {
                    todoList.get(i).setDraft(false);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    return requestedTodoList;
}

    public void synchroniseTodos() {
        todoListAdapter.printTodoList();
        boolean needToAddEmtyTodo = true;
        boolean refreshVue = false;
        List<Todo> todoList = todoListAdapter.getTodoList();

        for (int i = 0; i < todoList.size(); i++) {
            Todo todo = todoList.get(i);
            if (todo.getTitle().equals("")) {
                needToAddEmtyTodo = false;
            }
            if (todo.isDraft() && !todo.getTitle().equals("")) {
                todo.setDraft(false);
                todo.saveEventually();
                todo.pinInBackground();
                refreshVue = true;
            }
        }
        if (needToAddEmtyTodo) {
            Todo newTodo = new Todo();
            initEmptyTodo(newTodo);
            todoListAdapter.addItem(newTodo);
        }
        if(refreshVue == true) {
            todoListAdapter.notifyDataSetChanged();
        }
    }

    public void ifneedToAddEmptyTodo() {
        boolean needToAddEmptyTodo = true;
        List<Todo> todoList = todoListAdapter.getTodoList();
        for (int i = 0; i < todoList.size(); i++) {

            Todo todo = todoList.get(i);
            if (todo.getTitle().equals("")) {
                needToAddEmptyTodo = false;
            }


            if (needToAddEmptyTodo) {
                Todo newTodo = new Todo();
                initEmptyTodo(newTodo);
                todoListAdapter.addItem(newTodo);
            }
        }
    }

    public void initEmptyTodo(Todo newTodo) {
        ParseACL todoACL = new ParseACL();
        newTodo.setDraft(true);
        newTodo.setTitle("");
        newTodo.setTodoListName(todoListName);
        newTodo.setAuthor(ParseUser.getCurrentUser());
        newTodo.setUuidString();
        todoACL.setRoleReadAccess(todoListRole, true);
        todoACL.setRoleWriteAccess(todoListRole, true);
        newTodo.setACL(todoACL);
    }

    public void deleteList() {
        List<Todo> todoList = getTodos(true);
        for (int i = 0; i < todoList.size(); i++) {
            todoList.get(i).deleteEventually();
        }
        todoListRole.deleteEventually();
    }

    public void updateListInfo() {
        todolistTitleInfoView.setText(getString(R.string.list_title, todoListRole.getString(Todo.LIST_NAME_KEY)));
    }

    public String getTodoListName() {
        return todoListName;
    }

    public ParseRole getTodoListRole() {
        return todoListRole;
    }

    public void shareListWithUser(ParseUser user) {
        todoListRole.getUsers().add(user);
        todoListRole.getACL().setReadAccess(user, true);
        todoListRole.getACL().setWriteAccess(user, true);
        todoListRole.saveEventually();
    }

    public void setTodoListName(String todoListName) {
        this.todoListName = todoListName;
    }

    public void updateCache(Todo newTodo) {
        boolean update = false;
        for (int i = 0; i < todoList.size(); i++) {
            if (todoList.get(i).getUuidString().equals(newTodo.getUuidString())) {
                update = true;
                Log.d("Cache", "cache update,  cache: " + todoList.get(i).getTitle() + " newTodo: " + newTodo.getTitle());
            }
        }
        if (update == false) {
            todoList.add(newTodo);
        }
    }
    public void removeTodoFromCache (Todo todo) {
        todoList.remove(todo);
    }

    public void setTodoListRole(ParseRole todoListRole) {
        this.todoListRole = todoListRole;
    }

    public void sendNotificationToUsers(Todo todo) {
        List<ParseUser> users = null;
        try {
            users = todoListRole.getUsers().getQuery().find();
            for (int i = 0; i < users.size(); i++) {
                if (!users.get(i).getEmail().equals(ParseUser.getCurrentUser().getEmail())) {
                    Utils.sendPushToUser(users.get(i), todo.getTitle() + " ajouté à la liste" + todoListRole.getName());
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
}
