package example.com.nuuita;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by administrateur on 23/12/14.
 */
public class TodoListAdapter extends BaseAdapter {

    List<Todo> todoList;
    Context context;
    Button buttonOk;
    TodoListFragment fragment;
    Holder modifiedHolder;


    public TodoListAdapter(Context context, int resource, List<Todo> objects, Button buttonOk, TodoListFragment fragment) {
        this.context = context;
        todoList = new ArrayList<Todo>();
        todoList.addAll(objects);
        this.buttonOk = buttonOk;
        this.fragment = fragment;
    }

    public List<Todo> getTodoList() {
        return todoList;
    }

    @Override
    public int getCount() {
        return todoList.size();
    }

    @Override
    public Object getItem(int i) {
        return todoList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void addItem(Todo newTodo) {
        todoList.add(newTodo);
    }

    public void printTodoList() {
        for (int i = 0; i < todoList.size(); i++) {
            Log.d("TODOLIST", "[" + i + "] : " + todoList.get(i).getTitle() + " : " + todoList.get(i).getUuidString());
        }
    }

    @Override
    public View getView(final int position, View view, ViewGroup viewGroup) {
        final Holder holder;
        final Todo todo = todoList.get(position);
        String todoTitle = todo.getTitle();
        EditText todoTextView;
        ImageButton deleteButton;
        CheckBox isBuyCheckBox;

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_item_todo, null);
            todoTextView = (EditText) view.findViewById(R.id.todo_title);
            deleteButton = (ImageButton) view.findViewById(R.id.buttonDeleteTODO);
            isBuyCheckBox = (CheckBox) view.findViewById(R.id.checkBox);
            todoTextView.setTag(position);
            todoTextView.setText(todoTitle);
            holder = new Holder(todoTextView, todo, null, deleteButton, null,isBuyCheckBox);

            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
            todoTextView = holder.todoTextView;
            holder.todo = todo;
            todoTextView.setTag(position);
            todoTextView.removeTextChangedListener(holder.watcher);
            if (!todoTextView.getText().toString().equals(todoTitle)) {
                todoTextView.setText(todo.getTitle());
            }
        }
        int tag_position = (Integer) holder.todoTextView.getTag();
        holder.todoTextView.setId(tag_position);
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                todoList.get(position).deleteInBackground();
                fragment.removeTodoFromCache(todoList.get(position));
                todoList.remove(position);
                ifNeedToAddEmptyTodo();
                notifyDataSetChanged();

            }
        });

        holder.todoTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // Set an action for IME_ACTION_SEND :
                // 1) save the Todo
                // 2) close the keyboard
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    //
                    holder.todo.setTitle(holder.todoTextView.getText().toString());
                    holder.todo.saveEventually(newSavedTodoCallback(holder));
                    fragment.updateCache(holder.todo);
                    fragment.sendNotificationToUsers(holder.todo);
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    ifNeedToAddEmptyTodo();
                }
                return handled;
            }
        });

        holder.watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                modifiedHolder = holder;
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                final int position2 = holder.todoTextView.getId();
                final EditText Caption = (EditText) holder.todoTextView;
                if (Caption.getText().toString().length() > 0) {
                    todoList.get(position2).setDraft(true);
                    setItalicIfDraft(todoList.get(position2), Caption);
                } else {
                    final String please = ((Activity) context).getString(R.string.PleaseSome);
                    Toast.makeText(context, please, Toast.LENGTH_SHORT).show();
                }
                Log.d("Focus", "Save old title: "+ holder.todo.getTitle() + " New Title: " + holder.todoTextView.getText().toString());
                modifiedHolder = holder;
                modifiedHolder.todo.setTitle(Caption.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
        holder.touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int position = (Integer) v.getTag();
                Todo currentTodo = todoList.get(position);
                Log.d("Focus", "onTouchChange position: " + position);
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (modifiedHolder == null) {
                        //modifiedHolder = holder;
                        //Log.d("modifiedHolder", "No previous holder. Current : " + modifiedHolder.todo.getTitle());
                    } else if (modifiedHolder.todo.isDraft() && !modifiedHolder.todo.equals(currentTodo)) {
                        //User focus on another item
                        modifiedHolder.todo.saveEventually(newSavedTodoCallback(modifiedHolder));
                        fragment.updateCache(modifiedHolder.todo);
                        fragment.sendNotificationToUsers(modifiedHolder.todo);
                        Log.d("Focus", "Save : " + modifiedHolder.todo.getTitle());
                        ifNeedToAddEmptyTodo();
                        Log.d("modifiedHolder", "Previous holder : " + modifiedHolder.todo.getTitle() + " ID: " + modifiedHolder.todo.getUuidString() +
                                " Current : " + currentTodo.getTitle() + " ID: " + currentTodo.getUuidString());
                        //modifiedHolder = holder;
                    }
                }
                return false;
            }
        };
        holder.todoTextView.setOnTouchListener(holder.touchListener);

        holder.todoTextView.addTextChangedListener(holder.watcher);
        return view;
    }

    private SaveCallback newSavedTodoCallback(final Holder holder) {
        return new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    holder.todo.setDraft(false);
                    setItalicIfDraft(holder.todo, holder.todoTextView);
                    Log.d("Save Keyboard", "Save Eventually done ...");
                    Toast notif = Toast.makeText(context, holder.todo.getTitle() + " enregistré", Toast.LENGTH_SHORT);
                    notif.show();
                } else {
                    Toast notif = Toast.makeText(context, "Erreur lors de l'enregistrement de " + holder.todo.getTitle() + R.string.ProblemConnection, Toast.LENGTH_SHORT);
                    notif.show();
                }
            }
        };
    }

    public void setItalicIfDraft(Todo todo, EditText editText) {
        if (todo.isDraft()) {
            editText.setTypeface(null, Typeface.ITALIC);
        } else {
            editText.setTypeface(null, Typeface.NORMAL);
        }
    }

    public class Holder {
        EditText todoTextView;
        Todo todo;
        TextWatcher watcher;
        ImageButton delete;
        View.OnTouchListener touchListener;

        View.OnFocusChangeListener focusListener;
        CheckBox isBuyCheckBox;

        public Holder(EditText todoTextView, Todo todo, TextWatcher watcher, ImageButton delete , View.OnTouchListener touchListener, CheckBox isBuyCheckBox) {

            this.todo = todo;
            this.todoTextView = todoTextView;
            this.watcher = watcher;
            this.delete = delete;

            this.isBuyCheckBox = isBuyCheckBox;

            this.touchListener = null;

        }
    }

    public void checkIsDraft() {
        List<Todo> todoList = getTodoList();
        for (int i = 0; i < todoList.size(); i++) {

            Todo todo = todoList.get(i);

        }
    }
    public void ifNeedToAddEmptyTodo() {
        // Add a new Todo is needed
        boolean needToAddEmptyTodo = true;
        List<Todo> todoList = getTodoList();
        for (int i = 0; i < todoList.size(); i++) {

            Todo todo = todoList.get(i);
            if (todo.getTitle().equals("")) {
                needToAddEmptyTodo = false;
            }

        }
        if (needToAddEmptyTodo) {
            Todo newTodo = new Todo();
            ((TodoListActivity) context).getCurrentFragment().initEmptyTodo(newTodo);
            addItem(newTodo);

        }
    }
}