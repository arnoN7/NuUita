package example.com.nuuita;

import android.app.Fragment;
import android.os.Bundle;
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
import com.parse.ParseRole;
import com.parse.ParseUser;

import java.util.List;

/**
 * Created by Arnaud Rover on 26/12/14.
 */
public class TodoListFragment extends Fragment {

    // Adapter for the Todos Parse Query
    private TodoListAdapter todoListAdapter;
    public static final String TAG = "TodoListFragment";
    public static final String ROLE_NAME_KEY = "name";
    public static final String ARG_TODO_LIST_NAME = "todo_list_name";

    // For showing empty and non-empty todo views
    private Button buttonOk;
    private String todoListName;
    private ParseRole todoListRole;

    private TextView loggedInInfoView;
    private String groupDescription;
    private ParseACL groupACL;

    public static Fragment newInstance(ParseRole role) {
        Fragment fragment = new TodoListFragment();
        Bundle args = new Bundle();
        args.putString(TodoListFragment.ARG_TODO_LIST_NAME, role.getString(Todo.LIST_NAME_KEY));
        fragment.setArguments(args);
        ((TodoListFragment) fragment).setTodoListRole(role);
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
        todoListName = getArguments().getString(ARG_TODO_LIST_NAME);


        // If User is not Logged In Start Activity Login
        if (ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
            //TODO goto Login Activity
            //ParseLoginBuilder builder = new ParseLoginBuilder(getActivity());
            //startActivityForResult(builder.build(), TodoListActivity.LOGIN_ACTIVITY_CODE);
        }

        // Set up the views
        loggedInInfoView = (TextView) v.findViewById(R.id.loggedin_info);
        buttonOk = (Button) v.findViewById(R.id.buttonSetTODO);
        buttonOk.setVisibility(View.INVISIBLE);
        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                synchroniseTodos();
            }
        });
        updateLoggedInInfo();

        List<Todo> todoList = getTodos();
        //Add empty item at the end of the list
        Todo emptyTodo = new Todo();
        initEmptyTodo(emptyTodo);
        todoListAdapter = new TodoListAdapter(getActivity(), R.layout.list_item_todo, todoList, buttonOk);
        todoListAdapter.addItem(emptyTodo);
        // Attach the query adapter to the view
        ListView todoListView = (ListView) v.findViewById(R.id.todo_list_view);
        todoListView.setAdapter(todoListAdapter);

        return v;
    }

    private List<Todo> getTodos() {
        ParseQuery<Todo> query = Todo.getQuery();
        query.whereEqualTo(Todo.LIST_NAME_KEY, todoListName);
        List<Todo> todoList = null;
        try {
            todoList = query.find();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return todoList;
    }

    public void synchroniseTodos() {
        todoListAdapter.printTodoList();
        boolean needToAddEmtyTodo = true;
        List<Todo> todoList = todoListAdapter.getTodoList();

        for (int i = 0; i < todoList.size(); i++) {
            Todo todo = todoList.get(i);
            if (todo.getTitle().equals("")) {
                needToAddEmtyTodo = false;
            }
            if (todo.isDraft() && !todo.getTitle().equals("")) {
                todo.setDraft(false);
                ((TodoListActivity) getActivity()).getTaskQueue().add(todo);
            }
        }
        if (needToAddEmtyTodo) {
            Todo newTodo = new Todo();
            initEmptyTodo(newTodo);
            todoListAdapter.addItem(newTodo);
        }
        todoListAdapter.reloadNewImages();
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

    public void updateLoggedInInfo() {
        if (!ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
            ParseUser currentUser = ParseUser.getCurrentUser();
            loggedInInfoView.setText(getString(R.string.logged_in,
                    currentUser.getString("name")));
        } else {
            loggedInInfoView.setText(getString(R.string.not_logged_in));
        }
    }

    public String getTodoListName() {
        return todoListName;
    }

    public ParseRole getTodoListRole() {
        return todoListRole;
    }

    public void shareListWithUser(ParseUser user) {
        todoListRole.getUsers().add(user);
        todoListRole.getACL().setReadAccess(user,true);
        todoListRole.getACL().setWriteAccess(user,true);
        ((TodoListActivity)getActivity()).getTaskQueue().add(todoListRole);
    }

    public void setTodoListRole(ParseRole todoListRole) {
        this.todoListRole = todoListRole;
    }
}
