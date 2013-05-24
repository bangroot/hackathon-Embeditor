let s:IDEA_RPC_HOST="http://localhost"
let s:IDEA_RPC_PORT=63341

if !has('python')
	echo "Error: Required vim compiled with +python"
	finish
endif

" START RESOLVING
function! idea#resolve()
python << endpython
import vim
import xmlrpclib
server=xmlrpclib.ServerProxy(vim.eval("s:IDEA_RPC_HOST") + ":" + vim.eval("s:IDEA_RPC_PORT"))
# todo: col should consider tabwidth
(row, col) = vim.current.window.cursor
filepath = vim.eval("expand('%:p')")
row = row - 1
filecontent = '\n'.join(vim.current.buffer)
result = server.embeditor.resolve(filepath, filecontent, row, col)

# todo: replace with for and add popup
if len(result) > 0: 
    resolveResult = result[0]
    targetFilePath = resolveResult["path"]
    targetLine = resolveResult["line"] + 1
    targetColumn = resolveResult["column"] + 1
    # todo: add file existing checking
    if filepath != targetFilePath:
        vim.command(":e %s" % targetFilePath)
    if len(vim.current.buffer) >= targetLine:
        vim.current.window.cursor = (targetLine, 1)
        if len(vim.current.buffer[targetLine - 1]) >= targetColumn:
            vim.current.window.cursor = (targetLine, targetColumn)


endpython
endfunction

" redefine C-] for DEMO
nmap g] :call idea#resolve()<CR>
" END RESOLVING


" START COMPLETION
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
    result = server.embeditor.getCompletionStartOffsetInLine(filepath, filecontent, row, col)
else:
    result = server.embeditor.getCompletionVariants(filepath, filecontent, row, col)

vim.command("let result=%s" % result)
# todo: error handling
vim.command("return result")
endpython
endfunction

set completefunc=idea#complete
" END COMPLETION
