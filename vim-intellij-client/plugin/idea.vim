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
basestring = vim.eval("a:base")
filepath = vim.eval("expand('%:p')")
row = row - 1

firstlines = vim.current.buffer[:row]
currentline = vim.current.buffer[row][:col] + basestring + vim.current.buffer[row][col:]
lastlines = vim.current.buffer[row+1:]
filecontent = '\n'.join(firstlines) + '\n' + currentline + '\n' + '\n'.join(lastlines)

if vim.eval("a:findstart") == '1':
    result = server.embeditor.getCompletionPrefixLength(filepath, filecontent, row, col)
else:
    result = server.embeditor.getCompletionVariants(filepath, filecontent, row, col)

vim.command("let result=%s" % result)
# todo: error handling
vim.command("return result")
endpython
endfunction

set completefunc=idea#complete
