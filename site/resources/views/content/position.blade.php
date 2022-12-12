@extends('layouts.default')
@section('title', $data->Pst_name . ' - ' . trans('common.title'))
@section('site-description', $data->Pst_name)
@section('site-keywords', trans('page-keywords'))
{{-- @section('site-canonical', $canonical) --}}
{{-- @section('page-hreflang', $canonical) --}}
@section('article-headline', $data->Pst_name)
{{-- {{-- @section('article-date', $publishDate) --}}
@section('article-modification-date', $data->created_at)
@section('article-description', trans('page-description'))
@section('article-tag', '')


@section('content')
    @include('layouts.breadcrumb')

    <div class="row">
        <div class="col ">
            <h1 class="t-h1">{{ trans('common.search.position_sc') }}</h1>
        </div>
    </div>

    {{-- {{ddd($nav["data"])}} --}}
    <div class=" mb-5 m-article">

        <div class="row m-info">
            <div class="col-sm-12">

                <h2 class="">{{ $data->Pst_name }}</h2>
                <div class="">
                    {!! $data->Pst_body !!}
                </div>



                @if ($data->Pst_file != '/pub/files/')
                    <div class="mt-5">
                        <a href="/storage{{ $data->Pst_file }}" download="" class="">
                            <span class="m-badge-1">
                                <i class="cil-file"></i>
                            </span> Изтегли файл
                        </a>
                    </div>
                @endif



            </div>






        </div>

        <div id="calendar"></div>

        @php
            
            $period = \App\Http\Controllers\CalendarController::archivePeriod(App::getLocale(), 'm_position', Request::get('year'), Request::get('month'), null, null);
            
            $prefix = 'd';
            // dd($period);
        @endphp

        @if (count($period) > 0)
            @include('layouts.calendar_list')
        @endif

        @php
            
            $calendar = \App\Http\Controllers\CalendarController::archiveList(App::getLocale(), 'm_position', null, null);
            // dd($calendar);
        @endphp
        @include('layouts.calendar')



    </div>





@stop
