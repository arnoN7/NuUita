nclude <pebble.h>
#include "NuUita.h"

static Window *window;
static MenuLayer* todo_lists;

enum {
	MENU_ITEM_TEXT = 0, // Phone -> Watch: add list item text
	MENU_ITEM_ID = 1, // Phone -> Watch: add list item id
	MENU_KEY_ASK = 2, // Watch -> Phone: ask for menu lists
	MENU_LIST_ID = 3, // Watch -> Phone: ask for a list of items
};

#define MAX_TODO_LIST_COUNT (20)
#define MAX_TODO_ITEM_TEXT_LENGTH (16)

// Each list has a unique list_id
typedef struct
{
	int list_id;
	char text[MAX_TODO_ITEM_TEXT_LENGTH];
} TodoListItem;

static TodoListItem todo_list[MAX_TODO_LIST_COUNT];
static int todo_list_count = 0;

char appTitle[] = "NuUita";

// -- Todo list management --
static void todo_list_init(void)
{
	DictionaryIterator *iter;

	if (app_message_outbox_begin(&iter) != APP_MSG_OK)
	{
		return;
	}
	if (dict_write_uint8(iter, MENU_KEY_ASK, 1) != DICT_OK)
	{
		return;
	}
	app_message_outbox_send();
}

static TodoListItem* get_todo_list_item_at_index(int index)
{
	if (index < 0 || index >= MAX_TODO_LIST_COUNT)
	{
		return NULL;
	}

	return &todo_list[index];
}

static void todo_list_add(char *data, int list_id)
{
	if (todo_list_count == MAX_TODO_LIST_COUNT)
	{
		return;
	}

	strcpy(todo_list[todo_list_count].text, data);
	todo_list[todo_list_count].list_id = list_id;
	todo_list_count++;
}

// -- Menu Layer callback functions --
// Section header(there's only one header that serves as the application title)
static void draw_header_callback(GContext *ctx, const Layer *cell_layer, uint16_t section_index, void *callback_context)
{
	graphics_context_set_text_color(ctx, GColorBlack);
	graphics_draw_text(
			ctx,
			appTitle,
			fonts_get_system_font(FONT_KEY_GOTHIC_14_BOLD),
			GRect(0,0,layer_get_frame(cell_layer).size.w,
				layer_get_frame(cell_layer).size.h),
			GTextOverflowModeTrailingEllipsis,
			GTextAlignmentCenter,
			NULL);
}


//  Menu section header height fixed to default
static int16_t get_header_height_callback(struct MenuLayer *menu_layer, uint16_t section_index, void *callback_context)
{
	return MENU_CELL_BASIC_HEADER_HEIGHT;
}

// Menu cell height fixed to 44
static int16_t get_cell_height_callback(struct MenuLayer *menu_layer, MenuIndex *cell_index, void *data)
{
	return 44;
}

// Draw an individual row based on cell index
static void draw_row_callback(GContext* ctx, Layer *cell_layer, MenuIndex *cell_index, void *data)
{
	TodoListItem* item;
	const int index = cell_index->row;

	if ((item = get_todo_list_item_at_index(index)) == NULL)
	{
		return;
	}

	menu_cell_basic_draw(ctx, cell_layer, item->text, NULL, NULL);
}

// Returns the current todo list item count
static uint16_t get_num_rows_callback(struct MenuLayer *menu_layer, uint16_t section_index, void *data)
{
	return todo_list_count;
}

// Call the selected todo list
static void select_click_callback(MenuLayer *menu_layer, MenuIndex *cell_index, void *data)
{
	TodoListItem *item;
	const int index = cell_index->row;
	DictionaryIterator *iter;
	if ((item = get_todo_list_item_at_index(index)) == NULL)
	{
		return;
	}
	if (app_message_outbox_begin(&iter) != APP_MSG_OK)
	{
		return;
	}
	if (dict_write_int(iter, MENU_LIST_ID, &item->list_id, 4, true) != DICT_OK)
	{
		return;
	}
	app_message_outbox_send();
	list_items(iter, item->text); // envoie de l'iter attention je peux ne pas chercher au bon endroit
}

static void window_load(Window* window)
{
	Layer *window_layer = window_get_root_layer(window);
	GRect window_frame = layer_get_frame(window_layer);
	todo_lists = menu_layer_create(window_frame);
	menu_layer_set_callbacks(todo_lists, NULL, (MenuLayerCallbacks) 
			{
			.draw_header = (MenuLayerDrawHeaderCallback) draw_header_callback,
			.get_header_height = (MenuLayerGetHeaderHeightCallback) get_header_height_callback,
			.get_cell_height = (MenuLayerGetCellHeightCallback) get_cell_height_callback,
			.draw_row = (MenuLayerDrawRowCallback) draw_row_callback,
			.get_num_rows = (MenuLayerGetNumberOfRowsInSectionsCallback) get_num_rows_callback,
			.select_click = (MenuLayerSelectCallback) select_click_callback,
			});
	menu_layer_set_click_config_onto_window(todo_lists, window);
	layer_add_child(window_layer, menu_layer_get_layer(todo_lists));
}

static void in_received_handler(DictionaryIterator *iter, void *context) 
{
	Tuple *add_tuple_text = dict_find(iter, MENU_ITEM_TEXT);
	Tuple *add_tuple_id = dict_find(iter, MENU_ITEM_ID);

	if (add_tuple_text && add_tuple_id)
	{
		todo_list_add(add_tuple_text->value->cstring, add_tuple_id->value->int32);
	}

	menu_layer_reload_data(todo_lists);
}

static void app_message_init(void)
{
	// Init message buffers (message inbox size maximum, message outbox size maximum)
	app_message_open(64, 16);
	// Register message handlers
	app_message_register_inbox_received(in_received_handler);
}

int main(void)
{
	window = window_create();

	app_message_init();
	todo_list_init();

	// configure window
	window_set_window_handlers(window, (WindowHandlers)
			{
			.load = window_load,
			});
	window_stack_push(window, true); // Animated

	// init done, start event loop
	app_event_loop();

	// clean up
	window_destroy(window);
	menu_layer_destroy(todo_lists);
}

