<script>
import { onMount } from 'svelte'

let wsState = $state('not connected')
let wsSessionId = $state(null)

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
		wsSessionId = message.payload.sessionId
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
