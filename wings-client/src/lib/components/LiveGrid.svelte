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
            let topic:string = message.headers.topic;
            let event:string = topic.substring(topic.indexOf(':') + 1)
            if (event === 'entry') {
                rowData = [...rowData, message.payload]
                gridApi!.setGridOption('rowData', rowData);
            } else if (event === 'snapshot') {
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
