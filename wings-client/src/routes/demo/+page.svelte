<script>
import { onMount } from 'svelte'

let wsState = $state('not connected')
let wsSessionId = $state(null)
let messages = $state([])

onMount(() => {
    const socket = new WebSocket(`ws://${location.host}/websocket`);

    socket.addEventListener('open', event => {
		console.log('websocket opened')
        wsState = 'open'
    });

    socket.addEventListener('close', event => {
		console.log('websocket closed')
        wsState = 'closed'
    });

    socket.addEventListener('message', event => {
        let message = JSON.parse(event.data)
        if (message.headers.eventType == 'Websocket:init') {
		    wsSessionId = message.payload.sessionId
        } else {
            messages.push(event.data)
            messages = messages
        }
    });
})
</script>

<svelte:head>
	<title>About</title>
	<meta name="description" content="About this app" />
</svelte:head>

<div class="text-column">
	<p>WebSocket state: {wsState}</p>
	<p>WebSocket sessionId: {wsSessionId}</p>
</div>

{#each messages as message}
    <p>{message}</p>
{/each}
