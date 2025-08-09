package city.newnan.externalbook.i18n

/**
 * ExternalBook 语言键常量类
 *
 * 遵循五层架构的 i18n Key 分类体系，使用 <%key%> 格式便于模板替换。
 *
 * 五层架构分类：
 * 1. 核心系统层 (Core System Layer) - 插件生命周期、配置管理、系统级错误
 * 2. 命令系统层 (Command System Layer) - 命令处理、参数验证、帮助系统
 * 3. 图形界面层 (GUI Layer) - GUI界面、按钮、提示信息
 * 4. 业务领域层 (Business Domain Layer) - 书籍管理、事件处理
 * 5. 日志系统层 (Logging Layer) - 日志消息
 *
 * @author NewNanCity
 * @since 2.0.0
 */
object LanguageKeys {

    // ==================== 核心系统层 (Core System Layer) ====================
    object Core {
        object Plugin {
            const val RELOADED = "<%core.plugin.reloaded%>"
            const val RELOAD_FAILED = "<%core.plugin.reload_failed%>"
        }

        object Error {
            const val PLAYER_ONLY = "<%core.error.player_only%>"
            const val INVALID_UUID = "<%core.error.invalid_uuid%>"
        }

        object Common {
            const val INVALID_BOOK_ID = "<%core.common.invalid_book_id%>"
        }
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
        object Common {
            const val LIBRARIAN_NOT_AVAILABLE = "<%commands.common.librarian_not_available%>"
            const val LOG_LIBRARIAN_NOT_AVAILABLE = "<%commands.common.log_librarian_not_available%>"
            const val INVALID_BOOK_ID = "<%commands.common.invalid_book_id%>"
        }

        object Help {
            const val DESCRIPTION = "<%commands.help.description%>"
            const val QUERY = "<%commands.help.query%>"
        }

        object Main {
            const val DESCRIPTION = "<%commands.main.description%>"
        }

        object Give {
            const val DESCRIPTION = "<%commands.give.description%>"
            const val PLAYER = "<%commands.give.player%>"
            const val BOOK_ID = "<%commands.give.book_id%>"
            const val BOOK_NOT_FOUND = "<%commands.give.book_not_found%>"
            const val FAILED = "<%commands.give.failed%>"
            const val SUCCESS = "<%commands.give.success%>"
            const val SUCCESS_GET = "<%commands.give.success_get%>"
            const val INVENTORY_FULL = "<%commands.give.inventory_full%>"
            const val LOG_SUCCESS = "<%commands.give.log_success%>"
            const val LOG_FAILED = "<%commands.give.log_failed%>"
        }

        object Import {
            const val DESCRIPTION = "<%commands.import.description%>"
            const val INVALID_ITEM = "<%commands.import.invalid_item%>"
            const val NO_PERMISSION = "<%commands.import.no_permission%>"
            const val SUCCESS_NEW = "<%commands.import.success_new%>"
            const val SUCCESS_UPDATE = "<%commands.import.success_update%>"
            const val FAILED = "<%commands.import.failed%>"
            const val LOG_SUCCESS = "<%commands.import.log_success%>"
            const val LOG_FAILED = "<%commands.import.log_failed%>"
        }

        object Export {
            const val DESCRIPTION = "<%commands.export.description%>"
            const val INVALID_ITEM = "<%commands.export.invalid_item%>"
            const val NOT_FOUND = "<%commands.export.not_found%>"
            const val NOT_AUTHOR = "<%commands.export.not_author%>"
            const val SUCCESS = "<%commands.export.success%>"
            const val FAILED = "<%commands.export.failed%>"
            const val INVENTORY_FULL = "<%commands.export.inventory_full%>"
            const val LOG_OPENED = "<%commands.export.log_opened%>"
            const val LOG_FAILED = "<%commands.export.log_failed%>"
        }

        object Publish {
            const val DESCRIPTION = "<%commands.publish.description%>"
            const val INVALID_ITEM = "<%commands.publish.invalid_item%>"
            const val NOT_FOUND = "<%commands.publish.not_found%>"
            const val NOT_AUTHOR = "<%commands.publish.not_author%>"
            const val SUCCESS = "<%commands.publish.success%>"
            const val FAILED = "<%commands.publish.failed%>"
            const val INVENTORY_FULL = "<%commands.publish.inventory_full%>"
            const val LOG_SUCCESS = "<%commands.publish.log_success%>"
            const val LOG_FAILED = "<%commands.publish.log_failed%>"
        }

        object Strip {
            const val DESCRIPTION = "<%commands.strip.description%>"
            const val INVALID_ITEM = "<%commands.strip.invalid_item%>"
            const val NOT_FOUND = "<%commands.strip.not_found%>"
            const val NOT_AUTHOR = "<%commands.strip.not_author%>"
            const val SUCCESS = "<%commands.strip.success%>"
            const val FAILED = "<%commands.strip.failed%>"
            const val LOG_SUCCESS = "<%commands.strip.log_success%>"
            const val LOG_FAILED = "<%commands.strip.log_failed%>"
        }

        object Gui {
            const val DESCRIPTION = "<%commands.gui.description%>"
            const val OPENING = "<%commands.gui.opening%>"
            const val FAILED = "<%commands.gui.failed%>"
            const val LOG_OPENED = "<%commands.gui.log_opened%>"
            const val LOG_FAILED = "<%commands.gui.log_failed%>"
        }

        object Admin {
            const val DESCRIPTION = "<%commands.admin.description%>"
            const val GUI_OPENING = "<%commands.admin.gui_opening%>"
            const val GUI_FAILED = "<%commands.admin.gui_failed%>"
            const val LOG_FAILED = "<%commands.admin.log_failed%>"
        }

        object Reload {
            const val DESCRIPTION = "<%commands.reload.description%>"
            const val SUCCESS = "<%commands.reload.success%>"
            const val FAILED = "<%commands.reload.failed%>"
            const val LOG_SUCCESS = "<%commands.reload.log_success%>"
            const val LOG_FAILED = "<%commands.reload.log_failed%>"
        }

        object Open {
            const val DESCRIPTION = "<%commands.open.description%>"
            const val PLAYER = "<%commands.open.player%>"
            const val ULID = "<%commands.open.ulid%>"
            const val SUCCESS = "<%commands.open.success%>"
            const val FAILED = "<%commands.open.failed%>"
            const val BOOK_NOT_FOUND = "<%commands.open.book_not_found%>"
            const val LOG_SUCCESS = "<%commands.open.log_success%>"
            const val LOG_FAILED = "<%commands.open.log_failed%>"
        }
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        object Common {
            const val BACK = "<%gui.common.back%>"
            const val PREVIOUS_PAGE = "<%gui.common.previous_page%>"
            const val NEXT_PAGE = "<%gui.common.next_page%>"
            const val UNKNOWN_PLAYER = "<%gui.common.unknown_player%>"
        }

        object EditBook {
            const val TITLE_EDIT = "<%gui.edit_book.title_edit%>"
            const val TITLE_NEW = "<%gui.edit_book.title_new%>"
            const val UPLOAD_HINT = "<%gui.edit_book.upload_hint%>"
            const val INVENTORY_FULL_EXPORT = "<%gui.edit_book.inventory_full_export%>"
            const val INVENTORY_FULL_ORIGINAL = "<%gui.edit_book.inventory_full_original%>"

            object DownloadEditable {
                const val NAME = "<%gui.edit_book.download_editable.name%>"
                const val LORE = "<%gui.edit_book.download_editable.lore%>"
            }

            object ExportOriginal {
                const val NAME = "<%gui.edit_book.export_original.name%>"
                const val LORE = "<%gui.edit_book.export_original.lore%>"
            }

            object NewBookHint {
                const val LORE = "<%gui.edit_book.new_book_hint.lore%>"
            }

            object UploadButton {
                const val CANNOT_UPLOAD = "<%gui.edit_book.upload_button.cannot_upload%>"
                const val CAN_UPLOAD = "<%gui.edit_book.upload_button.can_upload%>"
                const val CHECKING = "<%gui.edit_book.upload_button.checking%>"
                const val CHECKING_HINT = "<%gui.edit_book.upload_button.checking_hint%>"
                const val NO_BOOK_HINT = "<%gui.edit_book.upload_button.no_book_hint%>"
                const val NO_LIBRARIAN_HINT = "<%gui.edit_book.upload_button.no_librarian_hint%>"
                const val WRONG_TYPE_HINT = "<%gui.edit_book.upload_button.wrong_type_hint%>"
                const val EXISTING_BOOK_NEW_MODE = "<%gui.edit_book.upload_button.existing_book_new_mode%>"
                const val EXISTING_BOOK_EDIT_MODE = "<%gui.edit_book.upload_button.existing_book_edit_mode%>"
                const val UPLOAD_HINT_NEW = "<%gui.edit_book.upload_button.upload_hint_new%>"
                const val UPLOAD_HINT_EDIT = "<%gui.edit_book.upload_button.upload_hint_edit%>"
                const val UNKNOWN_AUTHOR = "<%gui.edit_book.upload_button.unknown_author%>"
                const val UNTITLED = "<%gui.editbook.upload_button.untitled%>"
            }
        }

        object PlayerBooks {
            const val TITLE = "<%gui.player_books.title%>"
            const val ADD_MODIFY = "<%gui.player_books.add_modify%>"
            const val CLOSE = "<%gui.player_books.close%>"
            const val DELETE_CONFIRM_MESSAGE = "<%gui.player_books.delete_confirm_message%>"
            const val DELETE_INVENTORY_FULL = "<%gui.player_books.delete_inventory_full%>"
            const val DELETE_SUCCESS = "<%gui.player_books.delete_success%>"
            const val DELETE_CANCELLED = "<%gui.player_books.delete_cancelled%>"
            const val CHAT_INPUT_BUSY = "<%gui.player_books.chat_input_busy%>"

            object BookDisplay {
                const val CREATED_TIME = "<%gui.player_books.book_display.created_time%>"
                const val MODIFIED_TIME = "<%gui.player_books.book_display.modified_time%>"
                const val LAST_MODIFIER = "<%gui.player_books.book_display.last_modifier%>"
                const val ANONYMOUS = "<%gui.player_books.book_display.anonymous%>"
                const val LEFT_CLICK = "<%gui.player_books.book_display.left_click%>"
                const val RIGHT_CLICK = "<%gui.player_books.book_display.right_click%>"
                const val SHIFT_RIGHT_CLICK = "<%gui.player_books.book_display.shift_right_click%>"
                const val PREVIEW = "<%gui.player_books.book_display.preview%>"
                const val PREVIEW_CONTENT = "<%gui.player_books.book_display.preview_content%>"
                const val NO_CONTENT = "<%gui.player_books.book_display.no_content%>"
                const val ULID = "<%gui.player_books.book_display.ulid%>"
            }
        }

        object OnlinePlayers {
            const val TITLE = "<%gui.online_players.title%>"
        }

        // GUI 通用操作和错误
        const val PLAYER_NOT_FOUND = "<%gui.player_not_found%>"
        const val BOOK_NOT_FOUND = "<%gui.book_not_found%>"
        const val SAVE_FAILED = "<%gui.save_failed%>"
        const val INVENTORY_FULL = "<%gui.inventory_full%>"
        const val OPERATION_FAILED = "<%gui.operation_failed%>"
        const val DELETE_FAILED = "<%gui.delete_failed%>"
    }

    // ==================== 业务领域层 (Business Domain Layer) ====================
    object Events {
        const val BOOK_NOT_FOUND = "<%events.book_not_found%>"
        const val BOOK_OPEN_FAILED = "<%events.book_open_failed%>"
        const val BOOK_QUERY_FAILED = "<%events.book_query_failed%>"
    }

    object Book {
        const val UNKNOWN_AUTHOR = "<%book.unknown_author%>"
        const val WRITTEN_BOOK_LORE = "<%book.written_book_lore%>"
        const val WRITTEN_BOOK_WITH_MODIFY_INFO = "<%book.written_book_with_modify_info%>"
        const val WRITABLE_BOOK_LORE = "<%book.writable_book_lore%>"
        const val WRITABLE_BOOK_WITH_MODIFY_INFO = "<%book.writable_book_with_modify_info%>"
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {

    }
}