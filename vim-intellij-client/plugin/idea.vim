let s:IDEA_RPC_HOST="http://localhost"
let s:IDEA_RPC_PORT=63341

if !has('python')
	echo "Error: Required vim compiled with +python"
	finish
endif

function! idea#complete(findstart, base)
python << endpython
import vim
import xmlrpclib
server=xmlrpclib.ServerProxy(vim.eval("s:IDEA_RPC_HOST") + ":" + vim.eval("s:IDEA_RPC_PORT"))
# todo: col should consider tabwidth
(row, col) = vim.current.window.cursor
filepath = vim.eval("expand('%:p')")

if vim.eval("a:findstart") == '1':
    result = server.embeditor.getStartCompletionOffset(filepath, row - 1, col)
else:
    # todo: send whole file to server
    result = server.embeditor.getCompletionVariants(filepath, row - 1, col)

vim.command("let result=%s" % result)
# todo: error handling
vim.command("return result")
endpython
endfunction

set completefunc=idea#complete
