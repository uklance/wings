<script lang="ts">
    import { onMount, onDestroy } from 'svelte'
    import { subscribe, Subscription, type MessageListener } from '$lib/api.svelte'    
    import { createGrid, type GridApi, type GridOptions } from 'ag-grid-community';
    
    let { entity, gridOptions } = $props<{entity:string, gridOptions:GridOptions}>();

    let subscription:Subscription | undefined = undefined;
    let gridDiv:HTMLDivElement | undefined = $state();
    let gridApi:GridApi | undefined = $state();
    let rowData:any[] = $state([]);

    onMount(() => {
        gridApi = createGrid(gridDiv!, gridOptions);
        let messageListener: MessageListener = message => {
            if (message.headers.topic === entity + ':entry') {
                rowData = [...rowData, message.payload]
                gridApi!.setGridOption('rowData', rowData);
            } else if (message.headers.topic === entity + ':snapshot') {
                rowData = [...rowData, ...message.payload]
                gridApi!.setGridOption('rowData', rowData);
            } else {
                console.log(`Unsupported message ${JSON.stringify(message)}`)
            }
        };
        subscribe(entity, null).then(sub => {
            sub.addMessageListener(messageListener);
            subscription = sub;
        });
    });    
    onDestroy(() => {
        subscription?.close();
    })
</script>

<div bind:this={gridDiv} class="ag-theme-quartz" style="height: 500px" />
