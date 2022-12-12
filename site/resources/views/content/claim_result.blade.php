@extends('layouts.default')
@section('title', $data->ArL_title . ' - ' . trans('common.title'))
@section('site-description', $data->Ar_name)
@section('site-keywords', trans('page-keywords'))
{{-- @section('site-canonical', $canonical) --}}
{{-- @section('page-hreflang', $canonical) --}}
@section('article-headline', $data->ArL_title)
{{-- {{-- @section('article-date', $publishDate) --}}
@section('article-modification-date', $data->created_at)
@section('article-description', trans('page-description'))
@section('article-tag', '')


@section('content')

    <div class="row ah-d">
        <div class="col ">
            @include('layouts.breadcrumb')



            <h1 class="t-h1">
                {{-- {{ $req }} --}}

                @if ($error['submit'])
                    <i class="cis-sync-problem"></i> {{ trans('common.error.errorRest') }}
                    {{-- {{ $req }} --}}
                @else
                    <i class="cis-check text-success"></i> {{ trans('common.error.submitRest') }} "{{ $res['rnDoc'] }}"
                @endif





            </h1>


            <ul class="list-unstyled">

                @foreach ($info as $inf)
                    <li> <i class="cis-ban text-success"></i> </i> {{ $inf }}</li>
                @endforeach
                @if (count($req))
                    @foreach ($req as $err)
                        @foreach ($err as $er)
                            <li> <i class="cis-x text-danger"> </i> {{ $er }}</li>
                        @endforeach
                    @endforeach

                @endif
            </ul>

        </div>
    </div>







@stop
