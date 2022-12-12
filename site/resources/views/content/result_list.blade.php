@extends('layouts.default')
@section('title', ' Резултат от търсенето - ' . trans('common.title'))
{{-- @section('site-description', $data->Ar_name) --}}
@section('site-keywords', trans('page-keywords'))
{{-- @section('site-canonical', $canonical) --}}
{{-- @section('page-hreflang', $canonical) --}}
{{-- @section('article-headline', $data->ArL_title) --}}
{{-- {{-- @section('article-date', $publishDate) --}}
{{-- @section('article-modification-date', $data->created_at) --}}
@section('article-description', trans('page-description'))
@section('article-tag', '')


@section('content')

    <div class="row">
        <div class="col ">
            <h1 class="t-h1">{{ trans('common.search.H1') }}</h1>
        </div>
    </div>

    <div class="mt-4 mb-2">
        @if ($count == 0)
            {{ trans('common.search.result0') }} <strong><em>{{ $key }}</em></strong>
        @elseif ($count == 1)
            {{ trans('common.search.result1') }} {{ $count }} {{ trans('common.search.result2') }}
            <strong><em>{{ $key }}</em></strong>.
        @else
            {{ trans('common.search.result1a') }} {{ $count }} {{ trans('common.search.result2a') }}
            <strong><em>{{ $key }}</em></strong>.
        @endif
    </div>


    {{-- @include('layouts.breadcrumb') --}}
    <div class="m-article mt-4">

        @foreach ($res as $s)
            {{-- {{dd($s->Mv_date)}} --}}
            <div class="row ev-list">
                <div class="col-sm-12">
                    <div class="ev-head">
                        <div class="title">

                            @if ($s->sHead != 'event')
                                <a href="/{{ App::getLocale() }}/{{ $s->slug }}/{{ $s->sPath }}">
                                    {{ $s->sTitle }}
                                </a>
                            @else
                                {{ $s->sTitle }}
                            @endif



                        </div>
                        <div class="mt-2 m-date">

                            {{ df(App::getLocale(), $s->sDate, 0) }}
                        </div>


                        @if ($s->sHead != 'article')
                            <span class="badge badge-info">{{ trans('common.search.' . $s->sHead) }}</span>
                        @endif
                    </div>



                </div>





                {{-- {{ddd($s["media"])}} --}}


            </div>
        @endforeach
    </div>








@stop
