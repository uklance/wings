<script lang="ts">
	import { onMount } from 'svelte'
	import { websocketConnect, apiState } from '$lib/api.svelte'
	import LiveGrid from '$lib/components/LiveGrid.svelte'
	import type { GridOptions } from 'ag-grid-community';

	let wsState = $state('not connected')
	let wsSessionId = $state(null)
	let messages:String[] = $state([])

	let carGridOptions:GridOptions = {
		columnDefs: [
			{ field: 'carId'},
			{ field: 'make'},
			{ field: 'model'},
			{ field: 'trim'},
			{ field: 'year'}
		]
	};
	let houseGridOptions:GridOptions = {
		columnDefs: [
			{ field: 'houseId'},
			{ field: 'address'}
		]
	};

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

Cars<br />
<LiveGrid entity='Car' gridOptions={carGridOptions} /><br />

Houses<br />
<LiveGrid entity='House' gridOptions={houseGridOptions} /><br />