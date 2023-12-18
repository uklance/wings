<script lang="ts">
    import { onMount, onDestroy } from 'svelte'
    import { subscribe, Subscription, type MessageListener } from '$lib/api.svelte'    
    import { createGrid, type GridApi, type GridOptions } from 'ag-grid-community';
    
    export let entity:string;
    export let gridOptions:GridOptions;

    let subscription:Subscription;
    let gridDiv: HTMLDivElement;
    let gridApi:GridApi;
    let rowData:any[] = [];

    onMount(() => {
        gridApi = createGrid(gridDiv, gridOptions);
        subscription = subscribe(entity, null);
        subscription!.addMessageListener( message => {
            if (message.headers.topic === entity + ':entry') {
                rowData = [...rowData, message.payload]
                gridApi.setGridOption('rowData', rowData);
            } else if (message.headers.topic === entity + ':snapshot') {
                rowData = [...rowData, ...message.payload]
                gridApi.setGridOption('rowData', rowData);
            }
        });
    });
    onDestroy(() => {
        subscription?.close();
    })
</script>

<div bind:this={gridDiv} class="ag-theme-quartz" style="height: 500px" />
