import vim
import xmlrpclib

IDEA_RPC_HOST = 'http://localhost'
IDEA_RPC_PORT = 63341


def current_file_path():
    return vim.eval("expand('%:p')")


def resolve():
    file_path = current_file_path()
    row, col = get_caret_position()
    file_content = '\n'.join(vim.current.buffer)
    results = server().resolve(file_path, file_content, row, col)

    # todo: replace with for and add popup
    if len(results) > 0:
        result = results[0]
        path, line, column = resolve_result_values(result)
        if file_path != path:
            # todo: add file existing checking
            vim.command(":e %s" % path)
        set_caret_position(line, column)


def resolve_result_values(result):
    return result["path"], result["line"] + 1, result["column"]


def complete():
    find_start = int(vim.eval('a:findstart'))
    base_string = vim.eval('a:base')

    file_path = current_file_path()
    row, col = get_caret_position()
    first_lines = vim.current.buffer[:row]
    current_line = (vim.current.buffer[row][:col] +
                    base_string +
                    vim.current.buffer[row][col:])
    last_lines = vim.current.buffer[row + 1:]
    file_content = '\n'.join(['\n'.join(first_lines),
                              current_line,
                              '\n'.join(last_lines)])

    if find_start:
        result = server().getCompletionStartOffsetInLine(
            file_path, file_content, row, col)
    else:
        result = server().getCompletionVariants(
            file_path, file_content, row, col)

    vim.command('return %s' % result)


def server():
    return xmlrpclib.ServerProxy(IDEA_RPC_HOST + ":" + str(IDEA_RPC_PORT)).embeditor


def get_caret_position():
    # todo: col should consider tabwidth
    row, col = vim.current.window.cursor
    row = row - 1
    return row, col


def set_caret_position(line, column):
    if len(vim.current.buffer) >= line:
        vim.current.window.cursor = (line, 1)
        if len(vim.current.buffer[line - 1]) >= column:
            vim.current.window.cursor = (line, column)
