<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Str;

/**
 * @property int    $MnL_id
 * @property int    $Mn_id
 * @property int    $S_Lng_id
 * @property int    $created_at
 * @property int    $updated_at
 * @property int    $deleted_at
 * @property string $MnL_path
 * @property string $MnL_title
 * @property string $MnL_intro
 * @property string $MnL_body
 * @property string $MnL_meta
 */
class MNewsLng extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_news_lng';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'MnL_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Mn_id', 'S_Lng_id', 'MnL_path', 'MnL_title', 'MnL_intro', 'MnL_body', 'MnL_meta', 'St_id', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'MnL_id' => 'int', 'Mn_id' => 'int', 'S_Lng_id' => 'int', 'MnL_path' => 'string', 'MnL_title' => 'string', 'MnL_intro' => 'string', 'MnL_body' => 'string', 'MnL_meta' => 'string', 'St_id' => 'int', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();


        static::creating(function ($article) {
            $truncated = substr($article->MnL_title, 0, 80) . "-" . $article->Mn_id;
            if ($article->S_Lng_id == 2) {
                $truncated = substr($article->MnL_title, 0, 80);
                $article->MnL_path =  Str::slug(C2L($truncated));
            }
            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {


            $truncated = substr($article->MnL_title, 0, 80) . "-" . $article->Mn_id;
            $article->MnL_path = Str::slug(C2L($truncated));
            $article->updated_at = now();
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...

    public function eq_lng()
    {
        return $this->belongsTo(SLang::class, 'S_Lng_id');
    }
    public function eq_news()
    {
        return $this->belongsTo(MNews::class, 'Mn_id');
    }

    public function eq_files_news()
    {
        return $this->hasMany(MFiles::class, 'MnL_id');
    }
}
