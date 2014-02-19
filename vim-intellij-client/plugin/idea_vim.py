import vim
import xmlrpclib
import traceback  # for exception output

IDEA_RPC_HOST = "http://localhost"
IDEA_RPC_PORT = 63341


def current_file_path():
    return vim.eval("expand('%:p')")


def resolve():
    file_path = current_file_path()
    (row, col) = get_caret_position()
    file_content = '\n'.join(vim.current.buffer)
    result = server().resolve(file_path, file_content, row, col)

    # todo: replace with for and add popup
    if len(result) > 0:
        resolveResult = result[0]
        targetFilePath, targetLine, targetColumn = extract_values_from_resolve_outcome(resolveResult)
        if file_path != targetFilePath:
            # todo: add file existing checking
            vim.command(":e %s" % targetFilePath)
        set_caret_pos(targetLine, targetColumn)


def extract_values_from_resolve_outcome(resolve_outcome):
    return resolve_outcome["path"], resolve_outcome["line"] + 1, resolve_outcome["column"]


def complete():
    find_start = int(vim.eval('a:findstart'))
    base_string = vim.eval('a:base')

    (row, col) = get_caret_position()
    file_path = current_file_path()
    first_lines = vim.current.buffer[:row]
    current_line = vim.current.buffer[row][:col] + base_string + vim.current.buffer[row][col:]
    last_lines = vim.current.buffer[row + 1:]
    file_content = '\n'.join(['\n'.join(first_lines), current_line, '\n'.join(last_lines)])

    if find_start:
        result = server().getCompletionStartOffsetInLine(file_path, file_content, row, col)
    else:
        result = server().getCompletionVariants(file_path, file_content, row, col)

    vim.command('return %s' % result)


def server():
    return xmlrpclib.ServerProxy(IDEA_RPC_HOST + ":" + str(IDEA_RPC_PORT)).embeditor


def get_caret_position():
    # todo: col should consider tabwidth
    (row, col) = vim.current.window.cursor
    row = row - 1
    return row, col


def set_caret_pos(line, column):
    if len(vim.current.buffer) >= line:
        vim.current.window.cursor = (line, 1)
        if len(vim.current.buffer[line - 1]) >= column:
            vim.current.window.cursor = (line, column)
