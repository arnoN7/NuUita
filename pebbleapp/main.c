#include <pebble.h>
#include "NuUita.h"

static Window *loading_window = NULL, *main_window = NULL, *second_window = NULL;
static MenuLayer *todo_lists = NULL;
static MenuLayer *layer_list_items = NULL;
static TextLayer *loading_text_layer = NULL;

typedef enum {
	MENU_KEY_ASK = 2, /* Watch -> Phone: ask for the  menu of lists*/
	MENU_LIST_ID = 3, /* Watch -> Phone: ask for a specific list of items */
	ITEM_TEXT = 4, /* Phone ->Watch: add an item's text and id to a list */
	MENU_ITEM_TEXT = 5, /* Phone -> Watch: add an item text and id to the menu of lists */
} MsgTypes;

#define MAX_TODO_LIST_COUNT (20)
#define MAX_TODO_ITEM_TEXT_LENGTH (16)
#define MAX_LIST_COUNT (20)
#define MAX_ITEM_TEXT_LENGTH (16)

/* Each list has a unique list_id */
typedef struct
{
	int32_t list_id;
	char text[MAX_TODO_ITEM_TEXT_LENGTH];
} TodoListItem;

/* Each item has a unique item_id */
typedef struct
{
	int32_t item_id;
	char item_text[MAX_ITEM_TEXT_LENGTH];
} listItems;

static TodoListItem todo_list[MAX_TODO_LIST_COUNT];
static int todo_list_count = 0;

static listItems list_items[MAX_LIST_COUNT];
static int list_items_count = 0;

char appTitle[] = "NuUita"; /* Application Title */
char *listTitle; /* List Title */

static int loading = 0;

/************************************************************************************************* Second Window *************************************************************************************************/
/* -- list_items management --   */
static listItems* get_list_items_at_index(int index)
{
	if (index < 0 || index >= MAX_LIST_COUNT)
	{
		return NULL;
	}

	return &list_items[index];
}

static void list_items_add(char *data, int32_t item_id)
{
	if (list_items_count == MAX_LIST_COUNT)
	{
		return;
	}

	strcpy(list_items[list_items_count].item_text, data);
	list_items[list_items_count].item_id = item_id;
	list_items_count++;
}

/* deletes the content of the list_items array */
void delete_list_items_content()
{
	while (list_items_count >= 0)
	{
		list_items[list_items_count].item_id = 0;
		list_items[list_items_count].item_text[0] = '\0';
		list_items_count --;
	}
	list_items_count = 0;
}

/* -- list_items Menu Layer callback functions -- */
/* Section header(there's only one header that serves as the list title) */
static void second_draw_header_callback(GContext *ctx, const Layer *cell_layer, uint16_t section_index, void *callback_context)
{
	graphics_context_set_text_color(ctx, GColorBlack);
	graphics_draw_text(
			ctx,
			listTitle,
			fonts_get_system_font(FONT_KEY_GOTHIC_14_BOLD),
			GRect(0,0,layer_get_frame(cell_layer).size.w,
				layer_get_frame(cell_layer).size.h),
			GTextOverflowModeTrailingEllipsis,
			GTextAlignmentCenter,
			NULL);
}

/* list_items Menu section header fixed to basic */
static int16_t second_get_header_height_callback(struct MenuLayer *menu_layer, uint16_t section_index, void *callback_context)
{
	return MENU_CELL_BASIC_HEADER_HEIGHT;
}

/* list_items Menu cell height fixed to 44 */
static int16_t second_get_cell_height_callback(struct MenuLayer *menu_layer, MenuIndex *cell_index, void *data)
{
	return 44;
}

/* Draw an individual row based on cell index */
static void second_draw_row_callback(GContext* ctx, Layer *cell_layer, MenuIndex *cell_index, void *data)
{
	listItems* item;
	const int index = cell_index->row;

	if ((item = get_list_items_at_index(index)) == NULL)
	{
		return;
	}

	menu_cell_basic_draw(ctx, cell_layer, item->item_text, NULL, NULL);
}

/* Returns the current list items count */
static uint16_t second_get_num_rows_callback(struct MenuLayer *menu_layer, uint16_t section_index, void *data)
{
	return list_items_count;
}

/* Will later serve to make an action on a todo list item like checking a box or deleting it */
static void second_select_click_callback(MenuLayer *menu_layer, MenuIndex *cell_index, void *data)
{
	/* Add the ckeck box or a menu_basic cell marked as dirty */
}

static void second_window_load(Window* second_window) 
{
	/*APP_LOG(APP_LOG_LEVEL_INFO, "enter in second_window_load"); */

	/*configure layer_list_items menu */
	Layer *window_layer = window_get_root_layer(second_window);
	GRect window_frame = layer_get_frame(window_layer);
	layer_list_items = menu_layer_create(window_frame);
	menu_layer_set_callbacks(layer_list_items, NULL, (MenuLayerCallbacks) 
			{
			.draw_header = (MenuLayerDrawHeaderCallback) second_draw_header_callback,
			.get_header_height = (MenuLayerGetHeaderHeightCallback) second_get_header_height_callback,
			.get_cell_height = (MenuLayerGetCellHeightCallback) second_get_cell_height_callback,
			.draw_row = (MenuLayerDrawRowCallback) second_draw_row_callback,
			.get_num_rows = (MenuLayerGetNumberOfRowsInSectionsCallback) second_get_num_rows_callback,
			.select_click = (MenuLayerSelectCallback) second_select_click_callback
			});
	menu_layer_set_click_config_onto_window(layer_list_items, second_window);
	layer_add_child(window_layer, menu_layer_get_layer(layer_list_items));
	/* APP_LOG(APP_LOG_LEVEL_INFO, "quit in second_window_load"); */
}


/* congigure the loading_window */
void loading_window_load(Window *loading_window)
{
	/* create the loading_text_layer */
	loading_text_layer = text_layer_create(GRect(0, 60, 132, 168));
	text_layer_set_background_color(loading_text_layer, GColorClear);
	text_layer_set_text_color(loading_text_layer, GColorBlack);
	text_layer_set_text_alignment(loading_text_layer, GTextAlignmentCenter);
	text_layer_set_font(loading_text_layer, fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
	layer_add_child(window_get_root_layer(loading_window), text_layer_get_layer(loading_text_layer));
	text_layer_set_text(loading_text_layer, "Loading ...");

}

void get_list_items(int32_t item_id, char *item_title, Window *second_window, int32_t end_of_list)
{
	/* APP_LOG(APP_LOG_LEVEL_INFO, "enter in get_list_items"); */

	list_items_add(item_title, item_id);

	/* configure second_window */
	if (end_of_list == 1)
	{
		/* create and configure the second window with the menu */
		second_window = window_create();
		window_set_window_handlers(second_window, (WindowHandlers) {
				.load = second_window_load,
				});
		window_stack_push(second_window, NULL); /* No animated */
		loading = 0;
	}
	/* APP_LOG(APP_LOG_LEVEL_INFO, "quit in get_list_items");*/
}


/***************************************************************************** first window **************************************************************************************************************/

/* -- Todo list management --   */
static void todo_list_init(void)
{
	DictionaryIterator *iter;
	if (app_message_outbox_begin(&iter) != APP_MSG_OK)
	{
		return;
	}
	if (dict_write_uint8(iter, 0, MENU_KEY_ASK) != DICT_OK)
	{
		dict_write_end(iter);
		return;
	}
	dict_write_end(iter);
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

static void todo_list_add(char *data, int32_t list_id)
{
	if (todo_list_count == MAX_TODO_LIST_COUNT)
	{
		return;
	}

	strcpy(todo_list[todo_list_count].text, data);
	todo_list[todo_list_count].list_id = list_id;
	todo_list_count++;
}


/* -- todo_lists Menu Layer callback functions --  */
/* Section header(there's only one header that serves as the application title) */
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


/* todo_lists Menu section header height fixed to default */
static int16_t get_header_height_callback(struct MenuLayer *menu_layer, uint16_t section_index, void *callback_context)
{
	return MENU_CELL_BASIC_HEADER_HEIGHT;
}

/* todo_lists Menu cell height fixed to 44 */
static int16_t get_cell_height_callback(struct MenuLayer *menu_layer, MenuIndex *cell_index, void *data)
{
	return 44;
}

/* Draw an individual row based on cell index */
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

/* Returns the current todo list item count */
static uint16_t get_num_rows_callback(struct MenuLayer *menu_layer, uint16_t section_index, void *data)
{
	return todo_list_count;
}

/* Call the selected todo_list */
static void select_click_callback(MenuLayer *menu_layer, MenuIndex *cell_index, void *data)
{
	/* APP_LOG(APP_LOG_LEVEL_INFO, "enter in select_click_callback"); */

	TodoListItem *item;
	const int index = cell_index->row;
	DictionaryIterator *iter;
	int res = 0;

	if ((item = get_todo_list_item_at_index(index)) == NULL)
	{
		return;
	}
	if (app_message_outbox_begin(&iter) != APP_MSG_OK)
	{
		return;
	}
	if (dict_write_uint32(iter, 0, MENU_LIST_ID) != DICT_OK)
	{
		return;
	}

	if (dict_write_uint32(iter, 1, (uint32_t)item->list_id) != DICT_OK) 
	{

		return;
	}

	if (layer_list_items)
	{
		res = window_stack_remove(loading_window, NULL);
		window_destroy(second_window);
		second_window = NULL;
		(void)res;

		menu_layer_destroy(layer_list_items);
		layer_list_items = NULL;

	}

	delete_list_items_content();


	/* listTitle taken for the futur header of the list_items MenuLayer*/
	listTitle = (char *)malloc(sizeof(char) * (strlen(item->text) + 1));
	if (listTitle != NULL)
	{
		strcpy(listTitle, item->text);
		dict_write_end(iter);
		app_message_outbox_send();
	}
	else
		return;

	/* APP_LOG(APP_LOG_LEVEL_INFO, "quit in select_click_callback"); */
}

static void main_window_load(Window* main_window)
{
	/* APP_LOG(APP_LOG_LEVEL_INFO, "enter in window_load"); */

	/* configure menu_layer of the menu of lists */
	Layer *window_layer = window_get_root_layer(main_window);
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
	menu_layer_set_click_config_onto_window(todo_lists, main_window);
	layer_add_child(window_layer, menu_layer_get_layer(todo_lists));
	/* APP_LOG(APP_LOG_LEVEL_INFO, "quit in window_load"); */
}

void put_main_window()
{
	/* create and configure window */
	main_window = window_create();
	window_set_window_handlers(main_window, (WindowHandlers)
			{
			.load = main_window_load,
			});
	window_stack_push(main_window, true); /* Animated */
}
/*************************************************************************************  loading_window  ******************************************************************************************************/
/* removes loading_window */
void remove_loading_window(void)
{
	int res = 0;
	/* remove from the stack the loading_window */
	res = window_stack_remove(loading_window, NULL);
	(void)res;

	/* delete the loading_text_layer */
	text_layer_destroy(loading_text_layer);
	loading_text_layer = NULL;

	/* delete the loading window */
	window_destroy(loading_window);
	loading_window = NULL;

	loading = 0;
}

/* create and configure the laoding_window with a loading text layer */
void put_loading_window(void)
{

	loading_window = window_create();
	window_set_window_handlers(loading_window, (WindowHandlers) {
			.load = loading_window_load,
			});
	window_stack_push(loading_window, true); /* Animated */
	loading = 1;

}

/*********************************************************************************** AppMessage and main ****************************************************************************************************/
static void in_received_handler(DictionaryIterator *iter, void *context) 
{
	/* APP_LOG(APP_LOG_LEVEL_INFO, "enter in in_received_handler"); // Need to be commented before public release */
	int listId = 0;
	char *list_title = NULL;
	int item_id = 0;
	char *item_title = NULL;
	int end_of_list = 0;


	Tuple *msgid = dict_find(iter, 0);
	switch ((MsgTypes)(msgid->value->int32))
	{
		case MENU_ITEM_TEXT:
			listId = (dict_find(iter, 1))->value->int32;
			list_title = dict_find(iter, 2)->value->cstring;
			end_of_list = dict_find(iter, 3)->value->int32;
			/* APP_LOG(APP_LOG_LEVEL_INFO, "listId = %d", listId); // Need to be commented before public release */
			/* APP_LOG(APP_LOG_LEVEL_INFO, "list_title = %s", list_title); // Need to be commented before public release */
			todo_list_add(list_title, listId);
			if (end_of_list == 1)
			{
				put_main_window();
				remove_loading_window();
				end_of_list = 0;
			}
			break;
		case ITEM_TEXT:
			/* create and configure loading_window */
			if (loading_window == 0)
				put_loading_window();

			item_id = dict_find(iter, 1)->value->int32;
			item_title = dict_find(iter, 2)->value->cstring;
			end_of_list = dict_find(iter, 3)->value->int32;
			/* APP_LOG(APP_LOG_LEVEL_INFO, "item_id = %d", item_id); // Need to be commented before public release*/
			/* APP_LOG(APP_LOG_LEVEL_INFO, "item_title = %s", item_title); // Need to be commented before public release */
			get_list_items(item_id, item_title, second_window, end_of_list);
			if (end_of_list == 1)
			{
				end_of_list = 0;
				remove_loading_window();
			}
			break;
		default:
			return;
	}
	/* APP_LOG(APP_LOG_LEVEL_INFO, "quit in in_received_handlers"); // Need to be commented before public release */
}


void app_message_init(void)
{
	/* Init message buffers (message inbox size maximum, message outbox size maximum) */
	app_message_open(64, 64);
	/* Register message handlers */
	app_message_register_inbox_received(in_received_handler);

}


int main(void)
{
	if (loading_window == 0)
		put_loading_window();

	app_message_init();
	todo_list_init();

	/* init done, start event loop */
	app_event_loop();

	/* clean up */
	window_destroy(main_window);
	menu_layer_destroy(todo_lists);
}
