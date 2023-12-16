<script lang="ts">
import { onMount } from 'svelte'
import { websocketConnect, apiState } from '$lib/api.svelte'

let wsState = $state('not connected')
let wsSessionId = $state(null)
let messages:String[] = $state([])

onMount(() => websocketConnect())
</script>

<svelte:head>
	<title>About</title>
	<meta name="description" content="About this app" />
</svelte:head>

<div class="text-column">
	<p>WebSocket state: {apiState.socketState}</p>
	<p>WebSocket sessionId: {apiState.socketSessionId}</p>
</div>