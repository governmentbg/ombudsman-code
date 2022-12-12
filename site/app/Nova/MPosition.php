<?php

namespace App\Nova;

use Illuminate\Http\Request;
use Laravel\Nova\Fields\Date;
use Laravel\Nova\Fields\File;
use Laravel\Nova\Fields\ID;
use Laravel\Nova\Fields\Text;
use Laravel\Nova\Fields\Textarea;
use Laravel\Nova\Http\Requests\NovaRequest;
use Waynestate\Nova\CKEditor;
use Illuminate\Support\Str;
use Laravel\Nova\Fields\Select;

class MPosition extends Resource
{
    /**
     * The model the resource corresponds to.
     *
     * @var string
     */
    public static $model = \App\Models\MPosition::class;
    // public static $globallySearchable = false;

    /**
     * The single value that should be used to represent the resource when being displayed.
     *
     * @var string
     */
    public static $title = 'Pst_name';
    public static function label()
    {
        return 'Становища';
    }


    /**
     * The columns that should be searched.
     *
     * @var array
     */
    public static $search = [
        'Pst_name', 'Pst_body'
    ];

    /**
     * Get the fields displayed by the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function fields(Request $request)
    {
        return [
            ID::make(__('ID'), 'Pst_id')->sortable(),
            Text::make('Име', 'Pst_name')->sortable()->rules('required')->displayUsing(function ($text) {
                return Str::limit($text, 80);
            }),
            Select::make('Вид', 'Pst_doc_type')->options([
                1 => 'Становища',
                2 => 'Искания до Конституционния съд',
            ])->displayUsingLabels()->sortable(),

            File::make('Файл', 'Pst_file')->disk('public')->path('pub/files')->storeAs(function (Request $request) {
                return date('YmdGis') . '_' . $request->file('Pst_file')->getClientOriginalName();
            })->storeSize('Pst_size')->creationRules('required'),
            Date::make('Дата', 'Pst_date')->sortable(),
            Textarea::make('Описание', 'Pst_desc')->sortable()->hideFromIndex(),
            CKEditor::make('Съдържание', 'Pst_body')->displayUsing(function ($text) {
                return Str::limit($text, 500);
            }),
        ];
    }

    /**
     * Get the cards available for the request.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function cards(Request $request)
    {
        return [];
    }

    /**
     * Get the filters available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function filters(Request $request)
    {
        return [];
    }

    /**
     * Get the lenses available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function lenses(Request $request)
    {
        return [];
    }

    /**
     * Get the actions available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function actions(Request $request)
    {
        return [];
    }
}
